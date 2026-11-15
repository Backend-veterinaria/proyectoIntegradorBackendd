package com.example.proyectoIntegrador.enums;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppointmentStatusTest {

    @ParameterizedTest
    @CsvSource({
            "Programada, PROGRAMADA",
            "Agendada, PROGRAMADA",
            "Pendiente, PROGRAMADA",
            "Reprogramada, REPROGRAMADA",
            "Confirmada, CONFIRMADA",
            "Atendida, ATENDIDA",
            "No asistió, NO_ASISTIO",
            "NO ASISTIO, NO_ASISTIO",
            "Inasistencia, NO_ASISTIO",
            "Cancelada, CANCELADA",
            "Otra cosa, DESCONOCIDO",
    })
    void reconoceElEstadoPorLaDescripcion(String description, AppointmentStatus expected) {
        assertEquals(expected, AppointmentStatus.fromDescription(description));
    }

    @ParameterizedTest
    @NullSource
    void descripcionNulaEsDesconocido(String description) {
        assertEquals(AppointmentStatus.DESCONOCIDO, AppointmentStatus.fromDescription(description));
    }

    @ParameterizedTest
    @CsvSource({
            // Confirmar: solo citas programadas o reprogramadas
            "PROGRAMADA, CONFIRMADA, true",
            "REPROGRAMADA, CONFIRMADA, true",
            "CONFIRMADA, CONFIRMADA, false",
            // Atendida / no asistió: cualquier cita pendiente
            "PROGRAMADA, ATENDIDA, true",
            "CONFIRMADA, ATENDIDA, true",
            "REPROGRAMADA, NO_ASISTIO, true",
            "CONFIRMADA, NO_ASISTIO, true",
            // Estados finales: no cambian
            "CANCELADA, ATENDIDA, false",
            "ATENDIDA, NO_ASISTIO, false",
            "NO_ASISTIO, ATENDIDA, false",
            "ATENDIDA, CONFIRMADA, false",
            // Cancelar y reprogramar tienen sus propios endpoints
            "PROGRAMADA, CANCELADA, false",
            "PROGRAMADA, REPROGRAMADA, false",
            "CONFIRMADA, PROGRAMADA, false",
            // Estados no reconocidos
            "DESCONOCIDO, ATENDIDA, false",
            "PROGRAMADA, DESCONOCIDO, false",
    })
    void transiciones(AppointmentStatus from, AppointmentStatus to, boolean allowed) {
        assertEquals(allowed, from.canTransitionTo(to));
    }
}
