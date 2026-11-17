package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Emite y valida los JWT de acceso con JJWT (PRD Seguridad §12, §14.4; FR-SEC-AUTH-005/006).
 * <p>
 * Solo trabaja con el token: no consulta usuarios ni roles, que el filtro carga desde la base
 * de datos en cada petición (§7.1, §12.1). No escribe logs, para que ningún token llegue a
 * ellos (P4-03-j, §12.2).
 * <p>
 * Decisiones aplicadas:
 * <ul>
 *   <li>P4-03-f2: el instante de emisión se trunca a segundos completos y {@code exp} se calcula
 *       desde él. Así {@code exp - iat} es exactamente la duración configurada (JWT guarda las
 *       fechas en segundos enteros) y el {@code iat} nunca queda en el futuro.</li>
 *   <li>P4-03-e: solo se acepta HS256 (registro de algoritmos limitado), con firma, emisor y los
 *       claims {@code exp}, {@code iat}, {@code sub} y {@code jti} obligatorios; {@code iat} no
 *       puede ser futuro y {@code sub} debe ser un id positivo. JJWT rechaza por sí mismo los
 *       tokens sin firma ({@code alg=none}).</li>
 *   <li>P4-03-f: caducidad estricta; en el instante exacto de {@code exp} el token ya no es
 *       válido (RFC 7519; JJWT por sí solo lo aceptaría).</li>
 *   <li>P4-03-g: todas las fechas salen del {@link Clock} de la aplicación, sin margen de reloj.</li>
 * </ul>
 */
@Component
public class JwtService {

    private final Clock clock;

    private final String issuer;

    private final Duration accessTokenTtl;

    private final SecretKey key;

    private final JwtParser parser;

    public JwtService(JwtProperties properties, Clock clock) {
        this.clock = clock;
        this.issuer = properties.issuer();
        this.accessTokenTtl = properties.accessTokenTtl();
        // Patrón del PRD (§14.4). Keys.hmacShaKeyFor rechaza además las claves de menos de 256 bits.
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secretBase64()));
        this.parser = Jwts.parser()
                .sig().clear().add(Jwts.SIG.HS256).and()     // solo HS256 (P4-03-e)
                .verifyWith(key)
                .requireIssuer(issuer)
                .clock(() -> Date.from(clock.instant()))      // mismo reloj que la emisión (P4-03-g)
                .build();
    }

    /**
     * Emite un token de acceso para el usuario indicado (§12.1): {@code sub} = id persistente,
     * {@code iss}, {@code iat}, {@code exp} y un {@code jti} aleatorio. Sin roles en el token
     * (§12.1: se consultan en cada petición).
     *
     * @param userId id persistente del usuario ({@code users.id}); debe ser positivo
     * @return el token firmado con HS256 y su duración en segundos
     * @throws IllegalArgumentException si {@code userId} es nulo, cero o negativo: nunca se emite
     *                                  un token que {@link #extractUserId(String)} rechazaría
     */
    public AccessToken issueAccessToken(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be a positive id");
        }
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);   // P4-03-f2
        Instant expiresAt = issuedAt.plus(accessTokenTtl);

        String token = Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        return new AccessToken(token, accessTokenTtl.toSeconds());
    }

    /**
     * Valida el token y devuelve el id del usuario de su claim {@code sub}.
     *
     * @param token JWT compacto, sin el prefijo {@code Bearer}
     * @return el id persistente del usuario
     * @throws InvalidJwtException si el token no es válido por cualquier motivo; nunca lanza otra
     *                             excepción por un token mal formado (NFR-SEC-002)
     */
    public Long extractUserId(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidJwtException();
        }
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            Instant now = clock.instant();
            requireNotExpired(claims, now);
            requireIssuedAtNotInFuture(claims, now);
            requireId(claims);
            return userIdFrom(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            // Incluye ExpiredJwtException, SignatureException, MalformedJwtException,
            // UnsupportedJwtException, IncorrectClaimException y RequiredTypeException.
            throw new InvalidJwtException(ex);
        }
    }

    /** {@code exp} obligatorio y estricto: se rechaza si {@code now >= exp} (P4-03-f). */
    private static void requireNotExpired(Claims claims, Instant now) {
        Date expiration = claims.getExpiration();
        if (expiration == null || !now.isBefore(expiration.toInstant())) {
            throw new InvalidJwtException();
        }
    }

    /** {@code iat} obligatorio y no futuro (§12.1; JJWT no lo comprueba). */
    private static void requireIssuedAtNotInFuture(Claims claims, Instant now) {
        Date issuedAt = claims.getIssuedAt();
        if (issuedAt == null || issuedAt.toInstant().isAfter(now)) {
            throw new InvalidJwtException();
        }
    }

    /** {@code jti} obligatorio: todo token emitido por PulsePass lo lleva (§12.1). */
    private static void requireId(Claims claims) {
        String id = claims.getId();
        if (id == null || id.isBlank()) {
            throw new InvalidJwtException();
        }
    }

    /** {@code sub} obligatorio: un id de usuario entero y positivo (§12.1). */
    private static Long userIdFrom(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new InvalidJwtException();
        }
        try {
            long userId = Long.parseLong(subject);
            if (userId <= 0) {
                throw new InvalidJwtException();
            }
            return userId;
        } catch (NumberFormatException ex) {
            throw new InvalidJwtException(ex);
        }
    }
}
