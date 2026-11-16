package com.example.proyectoIntegrador.models;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Atención clínica registrada (HC-01): una entrada de la historia clínica de la mascota.
 */
@Data
public class ClinicalAttention {
    private Integer atencionId;
    private Integer agendaId;
    private Integer mascotaId;
    private String mascotaNombre;
    /** Dueño de la mascota. */
    private Integer clienteId;
    private Integer medicoId;
    private String medicoNombre;
    /** Fecha y hora en que se registró la atención (ISO, yyyy-MM-ddTHH:mm:ss). */
    private String fechaAtencion;
    private String motivoConsulta;
    private String diagnostico;
    private String procedimientos;
    private String observaciones;
    private BigDecimal pesoKg;
    private BigDecimal temperaturaC;
}
