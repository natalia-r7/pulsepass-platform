package com.pulsepass.controller;

import com.pulsepass.FixedClockTestConfiguration;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.enums.EventStatus;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.mapper.ArtistMapperImpl;
import com.pulsepass.mapper.EventMapperImpl;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.security.WebSecurityTestConfiguration;
import com.pulsepass.service.impl.AuthorizationServiceImpl;
import com.pulsepass.service.impl.EventServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.pulsepass.service.ServiceFixtures.cmf2026;
import static com.pulsepass.service.ServiceFixtures.venSmr01;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visibilidad de los eventos no publicados por HTTP (PRD Seguridad FR-SEC-EVT-02, T-SEC-017,
 * TC-SEC-004, TC-SEC-015; P4-D2, P4-D3; P4-04b).
 * <p>
 * A diferencia de {@code EventControllerTest}, aquí el servicio es real: {@link EventServiceImpl},
 * {@link AuthorizationServiceImpl} y los mappers de MapStruct, con la cadena de seguridad real.
 * Solo se simulan los repositorios. La identidad llega por la cadena de seguridad, sin simular el
 * contexto de seguridad. El {@code Clock} es el fijo de {@link FixedClockTestConfiguration},
 * compartido por todas las peticiones, así que los cuerpos de error se pueden comparar enteros.
 */
@WebMvcTest(EventController.class)
@Import({EventServiceImpl.class, AuthorizationServiceImpl.class, EventMapperImpl.class, ArtistMapperImpl.class,
        FixedClockTestConfiguration.class, WebSecurityTestConfiguration.class})
class EventDraftVisibilityTest {

    private static final String EVENT_CODE = "CMF-2026";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private VenueRepository venueRepository;

    @MockitoBean
    private ArtistRepository artistRepository;

    @ParameterizedTest(name = "{0} como {1}")
    @MethodSource("statusesByActor")
    @DisplayName("P4-D2 / P4-D3: solo PUBLISHED es público; STAFF y ADMIN ven cualquier estado; el resto recibe 404")
    void appliesVisibilityByStatusAndActor(EventStatus status, Actor actor) throws Exception {
        // ARRANGE
        Event event = cmf2026(venSmr01());
        event.setStatus(status);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        boolean visible = status == EventStatus.PUBLISHED || actor == Actor.STAFF || actor == Actor.ADMIN;

        // ACT
        ResultActions result = mockMvc.perform(as(actor, get("/api/events/{eventCode}", EVENT_CODE)));

        // ASSERT
        if (visible) {
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventCode").value(EVENT_CODE))
                    .andExpect(jsonPath("$.status").value(status.name()));
        } else {
            // Ningún dato del evento oculto: solo el ErrorResponse de «no existe».
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Event not found: " + EVENT_CODE))
                    .andExpect(jsonPath("$.eventCode").doesNotExist())
                    .andExpect(jsonPath("$.name").doesNotExist());
        }
    }

    @Test
    @DisplayName("T-SEC-017 / P4-D2: para un anónimo, el 404 de un borrador es idéntico al de un evento inexistente (mismo Clock fijo)")
    void hiddenDraftResponseIsIdenticalToMissingEventResponse() throws Exception {
        // ARRANGE: el mismo código existe primero en DRAFT y después no existe.
        Event draft = cmf2026(venSmr01());
        draft.setStatus(EventStatus.DRAFT);
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(draft), Optional.empty());

        // ACT
        MvcResult hidden = mockMvc.perform(get("/api/events/{eventCode}", EVENT_CODE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        MvcResult missing = mockMvc.perform(get("/api/events/{eventCode}", EVENT_CODE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        // ASSERT: cuerpos idénticos, incluido el timestamp del Clock fijo compartido.
        String hiddenBody = hidden.getResponse().getContentAsString();
        assertThat(hiddenBody)
                .contains("\"timestamp\":\"" + FixedClockTestConfiguration.EXPECTED_TIMESTAMP + "\"")
                .isEqualTo(missing.getResponse().getContentAsString());
    }

    // ------------------------------------------------------------------ ayudantes

    /** Los 5 estados de un evento por cada actor: 20 combinaciones. */
    static Stream<Arguments> statusesByActor() {
        return Stream.of(EventStatus.values())
                .flatMap(status -> Stream.of(Actor.values()).map(actor -> Arguments.of(status, actor)));
    }

    /** Añade la identidad del actor; el anónimo va sin autenticación. */
    private static MockHttpServletRequestBuilder as(Actor actor, MockHttpServletRequestBuilder builder) {
        if (actor.role == null) {
            return builder;
        }
        SecurityPrincipal principal = new SecurityPrincipal(
                actor.userId, actor.name().toLowerCase() + ".prueba", Set.of(actor.role));   // DATO DE PRUEBA
        return builder.with(authentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities())));
    }

    /** Actores de la visibilidad: visitante anónimo y los tres roles. */
    enum Actor {
        ANONYMOUS(null, 0L),
        USER(Role.USER, 1L),
        STAFF(Role.STAFF, 2L),
        ADMIN(Role.ADMIN, 3L);

        private final Role role;
        private final long userId;   // DATO DE PRUEBA

        Actor(Role role, long userId) {
            this.role = role;
            this.userId = userId;
        }
    }
}
