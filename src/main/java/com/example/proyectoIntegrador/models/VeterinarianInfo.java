package com.example.proyectoIntegrador.models;

import lombok.Data;

/**
 * Información de un veterinario que devuelven los endpoints de VET-01.
 */
@Data
public class VeterinarianInfo {
    /** Id en la tabla medico: es el que se usa al agendar citas (medicoId). */
    private Integer medicoId;
    private Integer personalId;
    /** Id de su usuario para iniciar sesión (tabla cliente); puede ser nulo en veterinarios antiguos. */
    private Integer clienteId;
    private String nombre;
    private String apellido;
    private String username;
    private String email;
    private String telefono;
    private String direccion;
    private Boolean activo;
}
