package com.pulsepass.mapper;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.response.VenueResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Convierte {@link Venue} en {@link VenueResponse} (PRD Servicios §33, NFR-004).
 *
 * <p>Todos los campos se llaman igual en la entidad y en el DTO, así que MapStruct los
 * copia sin {@code @Mapping}. Con {@code unmappedTargetPolicy = ERROR} (P2-05 D-E), la
 * compilación falla si algún campo del DTO queda sin valor.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VenueMapper {

    VenueResponse toResponse(Venue venue);
}
