package com.example.proyectoIntegrador.models;

import lombok.Data;

/**
 * Vacuna aplicada a una mascota (HC-05). Las fechas son ISO (yyyy-MM-dd).
 */
@Data
public class Vaccine {
    private Integer vacunaId;
    private Integer mascotaId;
    private String mascotaNombre;
    /** Dueño de la mascota. */
    private Integer clienteId;
    private Integer atencionId;
    private Integer medicoId;
    private String medicoNombre;
    private String nombre;
    private String lote;
    private String fechaAplicacion;
    private String proximaDosis;
    private String observaciones;
}
