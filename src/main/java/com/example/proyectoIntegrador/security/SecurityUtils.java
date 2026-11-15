package com.example.proyectoIntegrador.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Acceso al usuario autenticado y verificaciones de que la petición solo toca sus propios datos.
 * Los endpoints siguen recibiendo el id del cliente en la petición, pero se comprueba que sea
 * el mismo del token: así un cliente no puede consultar o modificar datos de otro cambiando el id.
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

    // TODO (roles, SEC-05): permitir que administradores y veterinarios accedan a datos de otros clientes.
    public static void requireSameClient(Integer clientId) {
        if (clientId == null || clientId != currentUser().userId()) {
            throw new AccessDeniedException("No puede acceder a datos de otro cliente");
        }
    }

    public static void requireSameUsername(String username) {
        if (username == null || !username.equals(currentUser().username())) {
            throw new AccessDeniedException("No puede acceder a datos de otro usuario");
        }
    }
}
