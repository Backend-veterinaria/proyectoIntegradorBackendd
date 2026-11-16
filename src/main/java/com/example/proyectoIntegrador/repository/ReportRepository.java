package com.example.proyectoIntegrador.repository;

import com.example.proyectoIntegrador.models.ReportRows;

import java.time.LocalDate;
import java.util.List;

/**
 * Consultas de los reportes (REP-01..03). Las fechas son inclusivas. medicoId nulo = todos los
 * veterinarios. No capturan excepciones.
 */
public interface ReportRepository {

    /** REP-01: cantidad de citas por estado con fecha en el período (todos los estados, aunque tengan 0). */
    List<ReportRows.AppointmentsByStatus> appointmentsByStatus(LocalDate desde, LocalDate hasta, Integer medicoId);

    /** REP-02: atenciones por veterinario en el período, de más a menos atenciones. */
    List<ReportRows.AttentionsByVeterinarian> attentionsByVeterinarian(LocalDate desde, LocalDate hasta, Integer medicoId);

    /** REP-03: mascotas atendidas en el período, de la atención más reciente a la más antigua. */
    List<ReportRows.AttendedPet> attendedPets(LocalDate desde, LocalDate hasta, Integer medicoId);
}
