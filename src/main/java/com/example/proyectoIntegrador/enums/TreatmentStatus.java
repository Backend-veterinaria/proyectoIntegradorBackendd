package com.example.proyectoIntegrador.enums;

/**
 * Estado de un tratamiento (HC-04). Se guarda con este mismo nombre en tratamiento.estado.
 */
public enum TreatmentStatus {
    EN_CURSO,
    FINALIZADO,
    SUSPENDIDO;

    /**
     * Seguimiento del tratamiento: uno en curso se puede finalizar o suspender; finalizado y
     * suspendido son estados finales (un tratamiento nuevo se registra aparte).
     */
    public boolean canTransitionTo(TreatmentStatus target) {
        return this == EN_CURSO && target != EN_CURSO;
    }
}
