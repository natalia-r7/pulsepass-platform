package com.pulsepass.mapper;

import com.pulsepass.domain.User;
import com.pulsepass.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Convierte {@link User} y su {@code UserProfile} en un único {@link UserResponse}
 * (PRD Servicios §33, NFR-004).
 *
 * <p>{@code id}, {@code username}, {@code email} y {@code active} salen de {@code User} sin
 * {@code @Mapping}. Los otros cinco campos salen de {@code user.profile}. Si el usuario no
 * tiene perfil, MapStruct deja esos campos en {@code null} sin lanzar excepción; la regla
 * «usuario sin perfil» (P2-D5) pertenece al servicio, no al mapper.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    @Mapping(target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName", source = "profile.lastName")
    @Mapping(target = "phone", source = "profile.phone")
    @Mapping(target = "city", source = "profile.city")
    @Mapping(target = "birthDate", source = "profile.birthDate")
    UserResponse toResponse(User user);
}
