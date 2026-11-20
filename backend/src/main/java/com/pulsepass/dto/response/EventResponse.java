package com.pulsepass.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;

/**
 * Vista completa de un evento (PRD Servicios §13).
 * <p>
 * No expone {@code Venue}, {@code Set<Artist>} ni {@code List<Ticket>}: del
 * venue solo da {@code venueCode} y {@code venueName}, y los artistas van como
 * {@code List<ArtistResponse>} (P2-D1).
 */
public record EventResponse(
        Long id,
        String eventCode,
        String name,
        String description,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        Integer minimumAge,
        String venueCode,
        String venueName,
        List<ArtistResponse> artists
) {
}
