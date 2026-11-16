package com.example.proyectoIntegrador.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Historia clínica de una mascota (HC-03): sus datos, sus atenciones, tratamientos y vacunas,
 * cada lista de lo más reciente a lo más antiguo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalHistory {
    private InfoMascota mascota;
    private List<ClinicalAttention> atenciones;
    private List<Treatment> tratamientos;
    private List<Vaccine> vacunas;
}
