package com.example.proyectoIntegrador.models;

import lombok.Data;

@Data
public class InfoMascota {
    private int idMascota;
    private String codigo;
    private String nombreMascota;
    private String tipoRaza;
    private int edad;
    private String genero;
    /** Id de la raza, para precargar el formulario de edición (CIT-02). */
    private Integer razaId;
    /** Dueño de la mascota. */
    private Integer clienteId;
    /** false si la mascota está inactiva (CIT-02). */
    private Boolean activo;
}
