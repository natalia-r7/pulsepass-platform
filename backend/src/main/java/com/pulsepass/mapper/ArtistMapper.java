package com.pulsepass.mapper;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.response.ArtistResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Convierte {@link Artist} en {@link ArtistResponse} (PRD Servicios §33, NFR-004).
 *
 * <p>Todos los campos se llaman igual en la entidad y en el DTO. {@link EventMapper} también
 * usa este mapper para convertir los artistas de un evento (P2-05 D-A).
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ArtistMapper {

    ArtistResponse toResponse(Artist artist);
}
