package com.example.proyectoIntegrador.security;

import java.util.Locale;

/**
 * Roles del sistema (RF-02). En la base de datos los roles están en la tabla rol; aquí se
 * reconocen por su descripción y no por su id, para no depender de los ids de cada base de datos.
 */
public enum Role {
    ADMIN,
    VETERINARIO,
    CLIENTE;

    /**
     * Reconoce el rol a partir de rol.descripcion (sin importar mayúsculas ni tildes en el resto del texto).
     * Si la descripción es nula o no se reconoce, se asigna CLIENTE: el rol con menos permisos.
     */
    public static Role fromDescription(String description) {
        if (description == null) {
            return CLIENTE;
        }
        String value = description.toUpperCase(Locale.ROOT);
        if (value.contains("ADMIN")) {
            return ADMIN;
        }
        if (value.contains("VETERIN") || value.contains("MEDIC") || value.contains("MÉDIC")) {
            return VETERINARIO;
        }
        return CLIENTE;
    }

    /**
     * Convierte el nombre guardado en el token o en InfoUser (ADMIN, VETERINARIO, CLIENTE).
     * Un valor nulo o desconocido (por ejemplo, un token emitido antes de existir los roles) es CLIENTE.
     */
    public static Role fromName(String name) {
        if (name == null) {
            return CLIENTE;
        }
        try {
            return Role.valueOf(name);
        } catch (IllegalArgumentException e) {
            return CLIENTE;
        }
    }

    /**
     * Administradores y veterinarios pueden consultar y gestionar datos de cualquier cliente.
     */
    public boolean isStaff() {
        return this == ADMIN || this == VETERINARIO;
    }

    /**
     * Nombre de la autoridad para Spring Security, usado por hasRole("ADMIN") y @PreAuthorize.
     */
    public String authority() {
        return "ROLE_" + name();
    }
}
