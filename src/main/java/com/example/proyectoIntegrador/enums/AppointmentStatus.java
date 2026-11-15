package com.example.proyectoIntegrador.enums;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Estados de una cita (RF-08). En la base de datos están en la tabla estado_cita; aquí se reconocen
 * por su descripción y no por su id, para no depender de los ids de cada base de datos
 * (mismo criterio que {@link com.example.proyectoIntegrador.security.Role}).
 */
public enum AppointmentStatus {
    PROGRAMADA,
    REPROGRAMADA,
    CONFIRMADA,
    ATENDIDA,
    NO_ASISTIO,
    CANCELADA,
    /** Descripción que no se reconoce: no admite cambios de estado. */
    DESCONOCIDO;

    /** Citas que todavía no se han atendido ni cancelado. */
    private static final Set<AppointmentStatus> PENDING = EnumSet.of(PROGRAMADA, REPROGRAMADA, CONFIRMADA);

    /**
     * Reconoce el estado a partir de estado_cita.descripcion, sin importar mayúsculas ni tildes.
     */
    public static AppointmentStatus fromDescription(String description) {
        if (description == null) {
            return DESCONOCIDO;
        }
        String value = Normalizer.normalize(description, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);

        // El orden importa: "NO ASISTIO" antes que cualquier otro y "REPROGRAM" antes que "PROGRAM"
        if (value.contains("NO ASIST") || value.contains("NO_ASIST") || value.contains("INASIST")) {
            return NO_ASISTIO;
        }
        if (value.contains("CANCEL")) {
            return CANCELADA;
        }
        if (value.contains("ATENDID")) {
            return ATENDIDA;
        }
        if (value.contains("CONFIRM")) {
            return CONFIRMADA;
        }
        if (value.contains("REPROGRAM")) {
            return REPROGRAMADA;
        }
        if (value.contains("PROGRAM") || value.contains("AGENDAD") || value.contains("PENDIENT")) {
            return PROGRAMADA;
        }
        return DESCONOCIDO;
    }

    /**
     * Cita que todavía no se ha atendido ni cancelado: se puede cancelar o reprogramar.
     */
    public boolean isPending() {
        return PENDING.contains(this);
    }

    /**
     * Cambios de estado que puede hacer el personal (VET-03):
     * <ul>
     *   <li>Programada o reprogramada → confirmada.</li>
     *   <li>Programada, reprogramada o confirmada → atendida o no asistió.</li>
     * </ul>
     * Cancelada, atendida y no asistió son estados finales. Cancelar se hace con el endpoint de
     * cancelación y reprogramar con el de reprogramación, no con este.
     */
    public boolean canTransitionTo(AppointmentStatus target) {
        return switch (target) {
            case CONFIRMADA -> this == PROGRAMADA || this == REPROGRAMADA;
            case ATENDIDA, NO_ASISTIO -> PENDING.contains(this);
            default -> false;
        };
    }
}
