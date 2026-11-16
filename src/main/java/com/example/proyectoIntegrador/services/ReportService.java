package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.models.Report;
import com.example.proyectoIntegrador.models.ReportFilterDTO;
import com.example.proyectoIntegrador.models.ReportRows;
import com.example.proyectoIntegrador.security.AuthenticatedUser;

/**
 * Reportes (RF-12): el administrador ve todo (o un veterinario); el veterinario, solo sus datos.
 */
public interface ReportService {

    /** REP-01: citas por estado en un período. */
    Report<ReportRows.AppointmentsByStatus> appointmentsByStatus(AuthenticatedUser user, ReportFilterDTO filter);

    /** REP-02: atenciones por veterinario en un período. */
    Report<ReportRows.AttentionsByVeterinarian> attentionsByVeterinarian(AuthenticatedUser user, ReportFilterDTO filter);

    /** REP-03: mascotas atendidas en un período. */
    Report<ReportRows.AttendedPet> attendedPets(AuthenticatedUser user, ReportFilterDTO filter);
}
