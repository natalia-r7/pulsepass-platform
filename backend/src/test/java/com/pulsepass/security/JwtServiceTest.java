package com.pulsepass.security;

import com.pulsepass.config.ClockConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Pruebas unitarias de {@link JwtService} (PRD Seguridad §12, §14.4, §17.1; P4-03).
 * Sin Spring: el servicio se crea con su constructor, claves ficticias y un {@link Clock} fijo,
 * igual que las pruebas de los servicios de la Fase 2. Los tokens manipulados se construyen
 * con la API pública de JJWT.
 */
class JwtServiceTest {

    private static final String ISSUER = "pulsepass-api";

    /** DATO DE PRUEBA: claves ficticias escritas como texto (32 y 64 bytes), sin valor real. */
    private static final String KEY_32 = "jwt-service-unit-test-key-32byte";
    private static final String KEY_64 = KEY_32 + "jwt-service-unit-test-key-64byte";
    private static final String OTHER_KEY_32 = "another-unit-test-key-32-bytes!!";

    /** DATO DE PRUEBA: instante de emisión en segundos completos y su caducidad (PT15M). */
    private static final Instant NOW = Instant.parse("2026-11-17T17:00:00Z");
    private static final Instant EXPIRATION = NOW.plus(Duration.ofMinutes(15));

    private static final Long USER_ID = 42L;   // DATO DE PRUEBA

    @Test
    @DisplayName("FR-SEC-AUTH-005 / §12.1: emite un JWS HS256 con sub, iss, iat, exp y jti")
    void issuesHs256TokenWithExpectedClaims() {
        JwtService service = service(KEY_32, NOW);

        String token = service.issueAccessToken(USER_ID).token();

        Jws<Claims> jws = read(token, KEY_32, NOW);
        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("HS256");
        Claims claims = jws.getPayload();
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(NOW);
        assertThat(claims.getExpiration().toInstant()).isEqualTo(EXPIRATION);
        assertThatNoException().isThrownBy(() -> UUID.fromString(claims.getId()));
    }

    @ParameterizedTest(name = "userId {0}")
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("§12.1: no se emite un token para un userId nulo, cero o negativo (sub debe ser un id positivo)")
    void refusesToIssueTokenForInvalidUserId(Long invalidUserId) {
        JwtService service = service(KEY_32, NOW);

        assertThatThrownBy(() -> service.issueAccessToken(invalidUserId))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId must be a positive id");
    }

    @Test
    @DisplayName("§10.2: expiresInSeconds es 900 con access-token-ttl PT15M")
    void expiresInMatchesConfiguredTtl() {
        AccessToken accessToken = service(KEY_32, NOW).issueAccessToken(USER_ID);

        assertThat(accessToken.expiresInSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("§12.1: cada token tiene un jti distinto")
    void eachTokenHasDifferentId() {
        JwtService service = service(KEY_32, NOW);

        String first = read(service.issueAccessToken(USER_ID).token(), KEY_32, NOW).getPayload().getId();
        String second = read(service.issueAccessToken(USER_ID).token(), KEY_32, NOW).getPayload().getId();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("FR-SEC-AUTH-006: un token válido devuelve el id del usuario de su claim sub")
    void validTokenReturnsUserId() {
        JwtService service = service(KEY_32, NOW);
        String token = service.issueAccessToken(USER_ID).token();

        assertThat(service.extractUserId(token)).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("P4-03-f2: con un Clock con milisegundos, iat se trunca al segundo y exp = iat + 900 s exactos")
    void issuedAtIsTruncatedToSecondsAndExpirationIsExactTtl() {
        Instant withMillis = Instant.parse("2026-11-17T17:00:00.750Z");   // DATO DE PRUEBA
        AccessToken accessToken = service(KEY_32, withMillis).issueAccessToken(USER_ID);

        Claims claims = read(accessToken.token(), KEY_32, withMillis).getPayload();
        Instant iat = claims.getIssuedAt().toInstant();
        Instant exp = claims.getExpiration().toInstant();
        assertThat(iat).isEqualTo(Instant.parse("2026-11-17T17:00:00Z"));
        assertThat(exp).isEqualTo(Instant.parse("2026-11-17T17:15:00Z"));
        assertThat(Duration.between(iat, exp)).isEqualTo(Duration.ofSeconds(accessToken.expiresInSeconds()));
    }

    @Test
    @DisplayName("P4-03-f2: un token emitido con un Clock con milisegundos se acepta al momento (iat no es futuro)")
    void tokenIssuedWithMillisecondClockIsAcceptedImmediately() {
        JwtService service = service(KEY_32, Instant.parse("2026-11-17T17:00:00.750Z"));
        String token = service.issueAccessToken(USER_ID).token();

        assertThat(service.extractUserId(token)).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("P4-03-f: el token se acepta 1 ms antes de exp")
    void tokenIsAcceptedOneMillisecondBeforeExpiration() {
        String token = service(KEY_32, NOW).issueAccessToken(USER_ID).token();

        assertThat(service(KEY_32, EXPIRATION.minusMillis(1)).extractUserId(token)).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("P4-03-f / RFC 7519: el token se rechaza en el instante exacto de exp")
    void tokenIsRejectedAtExactExpiration() {
        String token = service(KEY_32, NOW).issueAccessToken(USER_ID).token();

        assertRejected(service(KEY_32, EXPIRATION), token);
    }

    @Test
    @DisplayName("T-SEC-007: el token se rechaza 1 ms después de exp")
    void tokenIsRejectedOneMillisecondAfterExpiration() {
        String token = service(KEY_32, NOW).issueAccessToken(USER_ID).token();

        assertRejected(service(KEY_32, EXPIRATION.plusMillis(1)), token);
    }

    @Test
    @DisplayName("FR-SEC-AUTH-006: un token de otro emisor se rechaza")
    void tokenFromAnotherIssuerIsRejected() {
        String token = validClaims(NOW).issuer("otro-emisor").signWith(key(KEY_32), Jwts.SIG.HS256).compact();

        assertRejected(service(KEY_32, NOW), token);
    }

    @Test
    @DisplayName("T-SEC-008: un token con el contenido manipulado (sub cambiado) se rechaza")
    void tokenWithTamperedPayloadIsRejected() {
        String token = service(KEY_32, NOW).issueAccessToken(USER_ID).token();
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forgedPayload = payload.replace("\"sub\":\"42\"", "\"sub\":\"43\"");
        assertThat(forgedPayload).isNotEqualTo(payload);
        String tampered = parts[0] + "." + base64Url(forgedPayload) + "." + parts[2];

        assertRejected(service(KEY_32, NOW), tampered);
    }

    @Test
    @DisplayName("T-SEC-008: un token con la firma alterada se rechaza")
    void tokenWithTamperedSignatureIsRejected() {
        String token = service(KEY_32, NOW).issueAccessToken(USER_ID).token();
        int lastDot = token.lastIndexOf('.');
        char[] chars = token.toCharArray();
        int middle = lastDot + (token.length() - lastDot) / 2;
        chars[middle] = chars[middle] == 'A' ? 'B' : 'A';

        assertRejected(service(KEY_32, NOW), new String(chars));
    }

    @Test
    @DisplayName("TC-AUTH-011: un token firmado con otra clave se rechaza")
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = service(OTHER_KEY_32, NOW).issueAccessToken(USER_ID).token();

        assertRejected(service(KEY_32, NOW), token);
    }

    @Test
    @DisplayName("§12.2: un token sin firma (alg=none) se rechaza")
    void unsecuredTokenIsRejected() {
        String unsecured = validClaims(NOW).compact();

        assertRejected(service(KEY_32, NOW), unsecured);
    }

    @Test
    @DisplayName("§12.2 / P4-03-e: un token HS512 firmado con la misma clave de 64 bytes se rechaza (solo HS256)")
    void tokenWithAnotherHmacAlgorithmIsRejected() {
        String hs512 = validClaims(NOW).signWith(key(KEY_64), Jwts.SIG.HS512).compact();

        assertRejected(service(KEY_64, NOW), hs512);
    }

    @Test
    @DisplayName("P4-03 (aclaración 2): con una clave de 64 bytes se firma explícitamente con HS256 y el token se verifica")
    void sixtyFourByteKeySignsAndVerifiesHs256() {
        JwtService service = service(KEY_64, NOW);

        String token = service.issueAccessToken(USER_ID).token();

        assertThat(read(token, KEY_64, NOW).getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(service.extractUserId(token)).isEqualTo(USER_ID);
    }

    @ParameterizedTest(name = "falta {0}")
    @ValueSource(strings = {"sub", "exp", "iat", "jti"})
    @DisplayName("§14.4 / P4-03-e: un token sin sub, exp, iat o jti se rechaza")
    void tokenWithMissingClaimIsRejected(String missingClaim) {
        JwtBuilder builder = Jwts.builder().issuer(ISSUER);
        if (!"sub".equals(missingClaim)) {
            builder.subject("42");
        }
        if (!"exp".equals(missingClaim)) {
            builder.expiration(Date.from(EXPIRATION));
        }
        if (!"iat".equals(missingClaim)) {
            builder.issuedAt(Date.from(NOW));
        }
        if (!"jti".equals(missingClaim)) {
            builder.id(UUID.randomUUID().toString());
        }
        String token = builder.signWith(key(KEY_32), Jwts.SIG.HS256).compact();

        assertRejected(service(KEY_32, NOW), token);
    }

    @ParameterizedTest(name = "sub «{0}»")
    @ValueSource(strings = {"abc", "0", "-5", " "})
    @DisplayName("§12.1 / P4-03-e: un sub que no es un id positivo se rechaza")
    void tokenWithInvalidSubjectIsRejected(String subject) {
        String token = validClaims(NOW).subject(subject).signWith(key(KEY_32), Jwts.SIG.HS256).compact();

        assertRejected(service(KEY_32, NOW), token);
    }

    @Test
    @DisplayName("§12.1 / P4-03-e: un token con iat en el futuro se rechaza")
    void tokenIssuedInTheFutureIsRejected() {
        String token = validClaims(NOW).issuedAt(Date.from(NOW.plusSeconds(1)))
                .signWith(key(KEY_32), Jwts.SIG.HS256).compact();

        assertRejected(service(KEY_32, NOW), token);
    }

    @ParameterizedTest(name = "token «{0}»")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "abc", "a.b.c", "Bearer x"})
    @DisplayName("NFR-SEC-002: un token nulo, vacío o mal formado lanza solo InvalidJwtException")
    void malformedTokenIsRejected(String token) {
        assertRejected(service(KEY_32, NOW), token);
    }

    @Test
    @DisplayName("SEC-009 / NFR-SEC-001: el rechazo es genérico y no expone el token; AccessToken oculta el token")
    void rejectionIsGenericAndTokenIsNeverExposed() {
        AccessToken accessToken = service(KEY_32, NOW).issueAccessToken(USER_ID);
        String token = accessToken.token();

        assertThatThrownBy(() -> service(KEY_32, EXPIRATION).extractUserId(token))
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage("Invalid access token")
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(token));
        assertThat(accessToken.toString()).doesNotContain(token);
    }

    // ------------------------------------------------------------------ ayudantes

    private static JwtService service(String keyText, Instant now) {
        JwtProperties properties = new JwtProperties(
                ISSUER,
                Base64.getEncoder().encodeToString(keyText.getBytes(StandardCharsets.UTF_8)),
                Duration.ofMinutes(15));
        return new JwtService(properties, Clock.fixed(now, ClockConfig.APPLICATION_ZONE));
    }

    private static SecretKey key(String keyText) {
        return Keys.hmacShaKeyFor(keyText.getBytes(StandardCharsets.UTF_8));
    }

    /** Claims válidos sin firmar todavía; cada prueba cambia lo que necesita. */
    private static JwtBuilder validClaims(Instant now) {
        return Jwts.builder()
                .subject("42")
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(15))))
                .id(UUID.randomUUID().toString());
    }

    /** Lee un token con la API de JJWT y un reloj fijo, para inspeccionar su cabecera y sus claims. */
    private static Jws<Claims> read(String token, String keyText, Instant now) {
        return Jwts.parser()
                .verifyWith(key(keyText))
                .clock(() -> Date.from(now))
                .build()
                .parseSignedClaims(token);
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertRejected(JwtService service, String token) {
        assertThatThrownBy(() -> service.extractUserId(token))
                .isExactlyInstanceOf(InvalidJwtException.class)
                .hasMessage("Invalid access token");
    }
}
