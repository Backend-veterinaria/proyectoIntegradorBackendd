package com.example.proyectoIntegrador.models;

import lombok.Data;

/**
 * Tratamiento de una mascota (HC-04). Las fechas son ISO (yyyy-MM-dd).
 */
@Data
public class Treatment {
    private Integer tratamientoId;
    private Integer mascotaId;
    private String mascotaNombre;
    /** Dueño de la mascota. */
    private Integer clienteId;
    private Integer atencionId;
    private Integer medicoId;
    private String medicoNombre;
    private String medicamento;
    private String dosis;
    private String frecuencia;
    private String indicaciones;
    private String fechaInicio;
    private String fechaFin;
    /** EN_CURSO, FINALIZADO o SUSPENDIDO. */
    private String estado;
}
