package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.enums.Role;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Carga la identidad, el estado, los roles y el hash de la contraseña de un usuario
 * (PRD Seguridad §7.1, §12.1; FR-SEC-AUTH-004, FR-SEC-AUTH-009; P4-05-a).
 * <ul>
 *   <li>{@link #loadPrincipalById(Long)}: la usa {@link JwtAuthenticationFilter} en cada petición con
 *       el {@code sub} del token. Los roles y el estado salen siempre de la base de datos, nunca del
 *       token (§12.1, SEC-011): un usuario desactivado pierde el acceso en su siguiente petición.</li>
 *   <li>{@link #loadUserByUsername(String)}: la usará el login (P4-07). Busca primero por username
 *       exacto y, si no, por email sin distinguir mayúsculas (P4-D13).</li>
 * </ul>
 * Coste (NFR-SEC-003, P4-05-j): el usuario y sus roles se cargan con un número fijo de consultas,
 * que no crece con el número de roles. No se crean cuentas ni se guardan tokens (§7.1).
 * <p>
 * Los mensajes de las excepciones son genéricos: nunca llegan al cliente (el 401 es siempre el
 * mismo, P4-D9) y no revelan si un usuario existe.
 * <p>
 * Efecto de ser el único bean {@code UserDetailsService}: la configuración de autenticación de
 * Spring Security monta con él un {@code AuthenticationManager} global (lo indica en el log con
 * «Global AuthenticationManager configured with UserDetailsService bean…»). Hasta P4-07 nadie lo
 * usa: formLogin y httpBasic están desactivados y {@link JwtAuthenticationFilter} no autentica
 * contraseñas. El {@code AuthenticationManager} explícito con BCrypt se define en P4-07, y el
 * {@code PasswordEncoder}, en P4-06.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /** Mensaje interno común: no distingue «no existe», «inactivo» ni «sin credencial». */
    static final String UNAVAILABLE = "User not available";

    private final UserRepository userRepository;

    private final UserCredentialRepository userCredentialRepository;

    private final UserRoleRepository userRoleRepository;

    public CustomUserDetailsService(UserRepository userRepository, UserCredentialRepository userCredentialRepository,
                                    UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * Identidad actual del usuario de un token (P4-05-e).
     *
     * @param userId id persistente del usuario ({@code sub} del JWT)
     * @return el principal con sus roles actuales; sin roles, un principal sin authorities
     * @throws UsernameNotFoundException si el usuario no existe
     * @throws DisabledException         si el usuario está inactivo
     */
    @Transactional(readOnly = true)
    public SecurityPrincipal loadPrincipalById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(UNAVAILABLE));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new DisabledException(UNAVAILABLE);
        }
        return principalOf(user);
    }

    /**
     * Usuario para el login por username o email (P4-D13). Si no tiene credencial (usuarios previos a
     * la Fase 4, P4-D11), se trata igual que un usuario inexistente. Un usuario inactivo se devuelve
     * con {@code isEnabled() == false}; lo rechazará el proveedor de autenticación (P4-07).
     *
     * @throws UsernameNotFoundException si no hay usuario con ese identificador o no tiene credencial
     */
    @Override
    @Transactional(readOnly = true)
    public LoginUser loadUserByUsername(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException(UNAVAILABLE);
        }
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElseThrow(() -> new UsernameNotFoundException(UNAVAILABLE));
        UserCredential credential = userCredentialRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException(UNAVAILABLE));
        return new LoginUser(principalOf(user), credential.getPasswordHash(), Boolean.TRUE.equals(user.getActive()));
    }

    /** Roles actuales en una sola consulta (P4-02-c). */
    private SecurityPrincipal principalOf(User user) {
        Set<Role> roles = Set.copyOf(userRoleRepository.findRolesByUserId(user.getId()));
        return new SecurityPrincipal(user.getId(), user.getUsername(), roles);
    }
}
