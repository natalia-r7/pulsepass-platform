package com.pulsepass.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cadena de seguridad HTTP de PulsePass (PRD Seguridad §7.1, §11, §14.3; P4-04-b).
 * <p>
 * API sin sesión (FR-SEC-AUTH-008): CSRF, formLogin, httpBasic, logout y requestCache
 * desactivados, porque no hay cookies ni redirecciones (§14.3.6). Las reglas de URL siguen la
 * matriz de §11 y deniegan por defecto: todo lo que no se abre expresamente exige autenticación
 * (SEC-001, P4-D7). Las rutas concretas van antes que los patrones generales (§14.3.4).
 * <p>
 * Los 401 y 403 los escriben {@link RestAuthenticationEntryPoint} y {@link RestAccessDeniedHandler}
 * con el {@code ErrorResponse} común. La visibilidad de los borradores la decide el servicio
 * (P4-04b). Las rutas de autenticación son {@code POST /api/auth/login}, pública, y
 * {@code GET /api/auth/me}, autenticada (P4-07-H). La propiedad de los recursos no se decide aquí:
 * la comprueban las reglas {@code @PreAuthorize} de los servicios, activadas en
 * {@link MethodSecurityConfig} (P4-08-A, P4-08-D).
 * <p>
 * {@link JwtAuthenticationFilter} (P4-05) se crea aquí y no es un bean, para que solo exista dentro
 * de esta cadena y se ejecute una sola vez (P4-05-f). Va antes de
 * {@code UsernamePasswordAuthenticationFilter} (§14.3).
 * <p>
 * El {@link PasswordEncoder} (P4-06-C) es BCrypt de fuerza 10 (P4-D10): genera hashes
 * {@code $2a$10$…} de 60 caracteres, que caben en {@code password_hash VARCHAR(100)}. Lo usa el
 * registro (P4-06) y el {@link AuthenticationManager} del login.
 * <p>
 * El {@link AuthenticationManager} explícito (P4-05-a, P4-07-D) es un {@link ProviderManager} con un
 * {@link DaoAuthenticationProvider} sobre {@link CustomUserDetailsService} y ese
 * {@code PasswordEncoder}. Se dejan sus valores por defecto: un usuario inexistente se informa como
 * credenciales incorrectas, se compara un hash ficticio para no revelar por tiempo si el usuario
 * existe y se borra el hash tras autenticar.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Fuerza (log2 de las rondas) de BCrypt (P4-D10). */
    static final int BCRYPT_STRENGTH = 10;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    public AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   JwtService jwtService,
                                                   CustomUserDetailsService userDetailsService) throws Exception {
        http
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService, authenticationEntryPoint),
                        UsernamePasswordAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Despacho interno de errores (/error): sin esto, un error fuera de MVC acabaría en 401.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // Autenticación (FR-SEC-AUTH-002, FR-SEC-AUTH-010; P4-07-H).
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        // Públicas (FR-SEC-VEN-01/02, FR-SEC-ART-01..03, FR-SEC-USR-01).
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/venues/**", "/api/artists/**").permitAll()
                        // STAFF/ADMIN (FR-SEC-TKT-04); antes que el patrón público /api/events/*.
                        .requestMatchers(HttpMethod.GET, "/api/events/*/tickets/paid").hasAnyRole("STAFF", "ADMIN")
                        // Públicas (FR-SEC-EVT-02/03/06): la visibilidad de los no publicados la decide el
                        // servicio (P4-D2, P4-04b); by-artist ya devuelve solo PUBLISHED (P2-D15).
                        .requestMatchers(HttpMethod.GET, "/api/events/published", "/api/events/by-artist", "/api/events/*")
                        .permitAll()
                        // ADMIN (FR-SEC-EVT-01/04/05).
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/events/*/publish").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/artists/*").hasRole("ADMIN")
                        // Tickets (FR-SEC-TKT-01, FR-SEC-TKT-06).
                        .requestMatchers(HttpMethod.POST, "/api/tickets").hasRole("USER")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/*/use").hasAnyRole("STAFF", "ADMIN")
                        // Resto, autenticado (SEC-001, P4-D7): consulta, búsqueda por email y cancelación de
                        // tickets y búsquedas de usuario, cuya propiedad se comprueba en P4-08.
                        .anyRequest().authenticated());
        return http.build();
    }
}
