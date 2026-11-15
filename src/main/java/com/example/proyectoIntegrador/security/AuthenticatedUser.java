package com.example.proyectoIntegrador.security;

/**
 * Usuario autenticado, obtenido del token JWT. Es el "principal" que queda en el
 * SecurityContext durante la petición.
 *
 * @param userId   id del cliente (claim "sub" del token)
 * @param username nombre de usuario (claim "username")
 */
public record AuthenticatedUser(int userId, String username) {
}
