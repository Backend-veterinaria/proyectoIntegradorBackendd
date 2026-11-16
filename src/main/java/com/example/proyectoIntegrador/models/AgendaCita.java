package com.example.proyectoIntegrador.models;

import lombok.Data;

/**
 * Cita en la lista de citas del cliente (y base del detalle de la cita).
 */
@Data
public class AgendaCita {
    private int agendaCitaId;
    private int mascotaId;
    private String mascotaNombre;
    /** Veterinario asignado (null si la cita no tiene). */
    private Integer medicoId;
    private String nombreVeterinario;
    private String fechaInicio;
    private String horaInicio;
    private String estado;
    private String descripcion;
    private int estadoCitaId;
    /** Motivo de cancelación (CIT-07), o null. */
    private String motivoCancelacion;
}
