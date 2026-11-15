package com.example.proyectoIntegrador.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Acceso al usuario autenticado y verificaciones de que la petición solo toca datos permitidos.
 * Los endpoints siguen recibiendo el id del cliente en la petición: un CLIENTE solo puede usar
 * su propio id, mientras que ADMIN y VETERINARIO pueden actuar sobre cualquier cliente
 * (por ejemplo, agendar o consultar citas en su nombre).
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new AccessDeniedException("No hay un usuario autenticado");
    }

    /**
     * Permite el acceso si el usuario es personal (ADMIN o VETERINARIO) o si el id es el suyo.
     */
    public static void requireSameClient(Integer clientId) {
        AuthenticatedUser user = currentUser();
        if (user.role().isStaff()) {
            return;
        }
        if (clientId == null || clientId != user.userId()) {
            throw new AccessDeniedException("No puede acceder a datos de otro cliente");
        }
    }

    /**
     * Permite el acceso si el usuario es personal (ADMIN o VETERINARIO) o si el username es el suyo.
     */
    public static void requireSameUsername(String username) {
        AuthenticatedUser user = currentUser();
        if (user.role().isStaff()) {
            return;
        }
        if (username == null || !username.equals(user.username())) {
            throw new AccessDeniedException("No puede acceder a datos de otro usuario");
        }
    }

    /**
     * Exige que el id sea el del propio usuario, sin excepción por rol. Se usa en recursos
     * personales, como el canal de notificaciones.
     */
    public static void requireOwnClient(Integer clientId) {
        if (clientId == null || clientId != currentUser().userId()) {
            throw new AccessDeniedException("Solo puede acceder a sus propios recursos");
        }
    }
}
