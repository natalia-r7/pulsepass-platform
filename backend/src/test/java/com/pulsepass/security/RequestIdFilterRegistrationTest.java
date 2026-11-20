package com.pulsepass.security;

import com.pulsepass.FixedClockTestConfiguration;
import com.pulsepass.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registro de {@link RequestIdFilter} y disponibilidad del request ID en los logs de los 401 y
 * 403 (NFR-SEC-006, P4-04-j).
 * <p>
 * Comprueba que el filtro tiene un único registro, con orden {@link Ordered#HIGHEST_PRECEDENCE}
 * (antes que la cadena de Spring Security), que no forma parte de {@link SecurityFilterChain} y
 * que el MDC llega a los manejadores 401 y 403: sus líneas de log, capturadas con
 * {@link OutputCaptureExtension}, llevan el request ID por {@code logging.pattern.correlation}.
 */
@WebMvcTest(controllers = RequestIdFilterRegistrationTest.MdcProbeController.class)
@Import({RequestIdFilterRegistrationTest.MdcProbeController.class, FixedClockTestConfiguration.class,
        WebSecurityTestConfiguration.class})
@ExtendWith(OutputCaptureExtension.class)
class RequestIdFilterRegistrationTest {

    /** Request ID entre corchetes al principio del patrón de correlación: «[uuid] ». */
    private static final String REQUEST_ID_IN_LOG = "\\[[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\] ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    @DisplayName("P4-04-j: RequestIdFilter no forma parte de SecurityFilterChain")
    void isNotPartOfSecurityFilterChain() {
        assertThat(securityFilterChain.getFilters()).noneMatch(RequestIdFilter.class::isInstance);
    }

    @Test
    @DisplayName("P4-04-j: hay un único registro, con HIGHEST_PRECEDENCE, antes que Spring Security (-100), y ningún bean Filter suelto")
    void isRegisteredOnceBeforeSpringSecurity() {
        @SuppressWarnings("rawtypes")
        List<FilterRegistrationBean> registrations = context.getBeansOfType(FilterRegistrationBean.class).values()
                .stream()
                .filter(registration -> registration.getFilter() instanceof RequestIdFilter)
                .toList();

        assertThat(registrations).hasSize(1);
        assertThat(registrations.get(0).getOrder())
                .isEqualTo(Ordered.HIGHEST_PRECEDENCE)
                .isLessThan(SecurityFilterProperties.DEFAULT_FILTER_ORDER);
        assertThat(context.getBeansOfType(RequestIdFilter.class)).isEmpty();
    }

    @Test
    @WithSecurityPrincipal(roles = Role.USER)
    @DisplayName("P4-04-j: el controlador ve un requestId en el MDC, distinto en cada petición, y se limpia al terminar")
    void requestIdIsAvailableAndUniquePerRequest() throws Exception {
        String first = mockMvc.perform(get("/probe-mdc/request-id"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(get("/probe-mdc/request-id"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThatNoException().isThrownBy(() -> UUID.fromString(first));
        assertThatNoException().isThrownBy(() -> UUID.fromString(second));
        assertThat(first).isNotEqualTo(second);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("NFR-SEC-006: el log del 401 lleva el request ID del MDC")
    void requestIdIsInUnauthorizedLog(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/probe-mdc/request-id"))
                .andExpect(status().isUnauthorized());

        assertThat(output.getOut())
                .containsPattern(Pattern.compile(REQUEST_ID_IN_LOG + ".*Authentication required: GET /probe-mdc/request-id"));
    }

    @Test
    @WithSecurityPrincipal(roles = Role.USER)
    @DisplayName("NFR-SEC-006: el log del 403 lleva el request ID del MDC y el id del usuario")
    void requestIdIsInForbiddenLog(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        assertThat(output.getOut())
                .containsPattern(Pattern.compile(REQUEST_ID_IN_LOG + ".*Access denied: POST /api/events userId=1"));
    }

    @Test
    @DisplayName("NFR-SEC-006 / NFR-SEC-001: los logs no incluyen la query string ni la cabecera Authorization")
    void logsNeverContainQueryStringOrAuthorizationHeader(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/probe-mdc/request-id")
                        .queryParam("email", "privado@pulsepass.test")                    // DATO DE PRUEBA
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-de-prueba"))      // DATO DE PRUEBA
                .andExpect(status().isUnauthorized());

        assertThat(output.getOut())
                .contains("Authentication required: GET /probe-mdc/request-id")
                .doesNotContain("privado@pulsepass.test")
                .doesNotContain("token-de-prueba");
    }

    /** Controlador que solo existe en esta prueba: devuelve el requestId que ve en el MDC. */
    @RestController
    @RequestMapping("/probe-mdc")
    static class MdcProbeController {

        @GetMapping("/request-id")
        String requestId() {
            return MDC.get(RequestIdFilter.MDC_KEY);
        }
    }
}
