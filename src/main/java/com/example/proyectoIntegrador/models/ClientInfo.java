package com.example.proyectoIntegrador.models;

import lombok.Data;

/**
 * Datos de un propietario (cliente) que devuelven los endpoints de CIT-03.
 */
@Data
public class ClientInfo {
    private Integer clienteId;
    private String nombre;
    private String apellido;
    private String direccion;
    private String telefono;
    private String email;
    private String username;
    private String fechaRegistro;
    /** false si el propietario está inactivo (cliente.estado_cliente). */
    private Boolean activo;
    /** ADMIN, VETERINARIO o CLIENTE. */
    private String role;
}
