package com.pulsepass.dto.response;

import java.time.LocalDateTime;

import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;

/**
 * Vista resumida de un evento para listados (PRD Servicios §8, §11:
 * {@code findPublishedEvents} y {@code findByArtist}).
 * <p>
 * Del venue solo expone su código y su nombre, no la entidad.
 */
public record EventSummaryResponse(
        Long id,
        String eventCode,
        String name,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        String venueCode,
        String venueName
) {
}
