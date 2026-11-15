package com.example.proyectoIntegrador.models;

import lombok.Data;

/**
 * Una cita de la agenda de un veterinario (VET-02).
 */
@Data
public class VeterinarianAgendaItem {
    private Integer agendaId;
    /** Veterinario asignado a la cita. */
    private Integer medicoId;
    private String fecha;
    private String hora;
    private String descripcion;
    private Integer estadoId;
    private String estado;
    private Integer clienteId;
    private String clienteNombre;
    private String clienteTelefono;
    private Integer mascotaId;
    private String mascotaNombre;
}
