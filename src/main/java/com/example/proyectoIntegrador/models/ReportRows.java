package com.example.proyectoIntegrador.models;

/**
 * Filas de los reportes (REP-01..03).
 */
public final class ReportRows {

    private ReportRows() {
    }

    /**
     * REP-01: citas en un estado.
     *
     * @param status estado reconocido (PROGRAMADA, CANCELADA, ...), según AppointmentStatus
     */
    public record AppointmentsByStatus(int estadoId, String estado, String status, long cantidad) {
    }

    /**
     * REP-02: atenciones registradas por un veterinario y número de mascotas distintas atendidas.
     */
    public record AttentionsByVeterinarian(int medicoId, String veterinario, boolean activo, long atenciones,
                                           long mascotas) {
    }

    /**
     * REP-03: mascota atendida en el período, con su número de atenciones y la fecha de la última (ISO).
     */
    public record AttendedPet(int mascotaId, String mascotaNombre, String raza, Integer clienteId,
                              String propietario, long atenciones, String ultimaAtencion) {
    }
}
