package com.pulsepass.mapper;

import com.pulsepass.domain.User;
import com.pulsepass.dto.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static com.pulsepass.service.ServiceFixtures.andrea;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del {@link UserMapper} real (P2-D9, NFR-004).
 *
 * <p>Sin Spring, sin base de datos y sin mocks: el mapper se obtiene con
 * {@code Mappers.getMapper(...)}. Los IDs se asignan con {@code ReflectionTestUtils}
 * (P2-05 D-B).
 */
class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    @DisplayName("NFR-004: User y su UserProfile se convierten en un único UserResponse")
    void mapsUserAndProfileFields() {
        // ARRANGE
        User user = andrea();
        user.getProfile().setPhone("3000000000");         // DATO DE PRUEBA
        user.getProfile().setCity("Santa Marta");         // DATO DE PRUEBA
        ReflectionTestUtils.setField(user, "id", 100L);   // DATO DE PRUEBA

        // ACT
        UserResponse response = userMapper.toResponse(user);

        // ASSERT
        assertThat(response).isEqualTo(new UserResponse(
                100L, "andrea", "andrea@email.com", true,
                "Andrea", "Rojas", "3000000000", "Santa Marta", LocalDate.of(2001, 6, 15)));
    }

    @Test
    @DisplayName("Un usuario sin perfil copia los campos de User y deja en null los del perfil")
    void leavesProfileFieldsNullWhenUserHasNoProfile() {
        // ARRANGE
        User user = new User("sinperfil", "sinperfil@email.com", true); // DATO DE PRUEBA
        ReflectionTestUtils.setField(user, "id", 101L);                  // DATO DE PRUEBA

        // ACT
        UserResponse response = userMapper.toResponse(user);

        // ASSERT
        assertThat(response).isEqualTo(new UserResponse(
                101L, "sinperfil", "sinperfil@email.com", true,
                null, null, null, null, null));
    }

    @Test
    @DisplayName("Un User null produce null")
    void returnsNullForNullUser() {
        // ACT + ASSERT
        assertThat(userMapper.toResponse(null)).isNull();
    }
}
