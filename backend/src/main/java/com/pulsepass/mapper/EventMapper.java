package com.pulsepass.mapper;

import com.pulsepass.domain.Event;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Convierte {@link Event} en {@link EventResponse} y {@link EventSummaryResponse}
 * (PRD Servicios §33, §37, NFR-004).
 *
 * <ul>
 *   <li>Del venue solo se copian {@code venue.code} y {@code venue.name} (§33).</li>
 *   <li>{@code artists} ({@code Set<Artist>}) se convierte en {@code List<ArtistResponse>}
 *       con {@link ArtistMapper} ({@code uses}). No se ordena (P2-05 D-C).</li>
 *   <li>{@code ArtistMapper} se recibe por constructor
 *       ({@code InjectionStrategy.CONSTRUCTOR}, P2-05 D-A). Así, en las pruebas se puede
 *       crear con {@code new EventMapperImpl(...)} sin Spring.</li>
 *   <li>{@code streamingUrl} no está en los DTO del PRD de Servicios y no se copia.</li>
 * </ul>
 */
@Mapper(componentModel = "spring",
        uses = ArtistMapper.class,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EventMapper {

    @Mapping(target = "venueCode", source = "venue.code")
    @Mapping(target = "venueName", source = "venue.name")
    EventResponse toResponse(Event event);

    /** El nombre {@code toSummary} es el que usa el PRD en §37 ({@code eventMapper::toSummary}). */
    @Mapping(target = "venueCode", source = "venue.code")
    @Mapping(target = "venueName", source = "venue.name")
    EventSummaryResponse toSummary(Event event);
}
