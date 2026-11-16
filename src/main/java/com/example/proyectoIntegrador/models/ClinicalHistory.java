package com.example.proyectoIntegrador.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Historia clínica de una mascota (HC-03): sus datos y sus atenciones, de la más reciente a la más antigua.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalHistory {
    private InfoMascota mascota;
    private List<ClinicalAttention> atenciones;
}
