package com.example.proyectoIntegrador.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resultado de un reporte (RF-12): el período consultado (ISO, ambas fechas incluidas), el veterinario
 * filtrado (o null si son todos), el total y las filas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report<T> {
    private String desde;
    private String hasta;
    private Integer medicoId;
    private long total;
    private List<T> items;
}
