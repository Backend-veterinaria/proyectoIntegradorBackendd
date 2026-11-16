package com.example.proyectoIntegrador.models;

/**
 * Datos de los avisos en tiempo real de una cita que cambió (eventos SSE "cita-cancelada" y
 * "cita-reprogramada"). Fecha y hora con el formato de la base de datos (yyyy-MM-dd, HH:mm:ss).
 *
 * @param motivo motivo de cancelación, o null
 */
public record AppointmentEvent(Integer agendaId, String descripcion, String fecha, String hora, String estado,
                               String mascotaNombre, String motivo) {
}
