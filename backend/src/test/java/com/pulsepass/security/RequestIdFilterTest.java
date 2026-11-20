package com.pulsepass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de {@link RequestIdFilter} (NFR-SEC-006, P4-04-j), sin Spring.
 */
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("NFR-SEC-006: durante la petición, el MDC contiene un requestId con formato UUID")
    void putsRequestIdInMdcDuringRequest() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        FilterChain chain = (request, response) -> seen.set(MDC.get(RequestIdFilter.MDC_KEY));

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(seen.get()).isNotNull();
        assertThatNoException().isThrownBy(() -> UUID.fromString(seen.get()));
    }

    @Test
    @DisplayName("P4-04-j: al terminar la petición, el requestId se elimina del MDC")
    void removesRequestIdAfterRequest() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) -> { });

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("P4-04-j: el requestId se elimina del MDC aunque la petición falle")
    void removesRequestIdWhenChainFails() {
        FilterChain failingChain = (request, response) -> {
            throw new ServletException("fallo de prueba");
        };

        assertThatThrownBy(() -> filter.doFilter(
                new MockHttpServletRequest(), new MockHttpServletResponse(), failingChain))
                .isInstanceOf(ServletException.class);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("P4-04-j: cada petición recibe un requestId distinto")
    void eachRequestGetsDifferentId() throws Exception {
        AtomicReference<String> first = new AtomicReference<>();
        AtomicReference<String> second = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> first.set(MDC.get(RequestIdFilter.MDC_KEY)));
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> second.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertThat(first.get()).isNotNull().isNotEqualTo(second.get());
    }
}
