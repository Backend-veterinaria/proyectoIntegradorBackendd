package com.example.proyectoIntegrador.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el token JWT de cada petición y, si es válido, deja al usuario autenticado en el
 * SecurityContext. Si no hay token o no es válido, la petición sigue sin autenticar y
 * Spring Security la rechaza con 401 cuando el endpoint lo requiere.
 * <p>
 * No es un @Component a propósito: se registra solo dentro de la cadena de Spring Security
 * (SecurityConfig), para que no se ejecute dos veces como filtro del servlet.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String BEARER_PREFIX = "Bearer ";
    // EventSource (SSE) en el navegador no permite enviar encabezados, así que para esa
    // conexión el token también se acepta como parámetro ?token=...
    static final String SSE_PATH = "/integrador/notifications/connection";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            jwtService.parseToken(token).ifPresent(user -> {
                var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (SSE_PATH.equals(path)) {
            return request.getParameter("token");
        }
        return null;
    }
}
