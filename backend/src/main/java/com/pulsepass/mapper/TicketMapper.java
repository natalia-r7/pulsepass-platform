package com.pulsepass.mapper;

import com.pulsepass.domain.Ticket;
import com.pulsepass.dto.response.TicketResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Convierte {@link Ticket} en {@link TicketResponse} (PRD Servicios §34, NFR-004).
 *
 * <p>Los campos propios del ticket se copian sin {@code @Mapping}. Del usuario y del evento
 * solo se copian los tres valores que pide §34; nunca las entidades.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TicketMapper {

    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "eventCode", source = "event.eventCode")
    @Mapping(target = "eventName", source = "event.name")
    TicketResponse toResponse(Ticket ticket);
}
