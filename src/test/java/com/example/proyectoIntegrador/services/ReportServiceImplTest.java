package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.Report;
import com.example.proyectoIntegrador.models.ReportFilterDTO;
import com.example.proyectoIntegrador.models.ReportRows;
import com.example.proyectoIntegrador.models.VeterinarianInfo;
import com.example.proyectoIntegrador.repository.ReportRepository;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.Impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final AuthenticatedUser VETERINARIO = new AuthenticatedUser(40, "dr.perez", Role.VETERINARIO);
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, "admin", Role.ADMIN);
    private static final AuthenticatedUser CLIENTE = new AuthenticatedUser(5, "ana", Role.CLIENTE);
    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOV_30 = LocalDate.of(2026, 11, 30);

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private VeterinarianRepository veterinarianRepository;

    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(reportRepository, veterinarianRepository);
    }

    private static ReportFilterDTO filter(String desde, String hasta, Integer medicoId) {
        ReportFilterDTO f = new ReportFilterDTO();
        f.setDesde(desde);
        f.setHasta(hasta);
        f.setMedicoId(medicoId);
        return f;
    }

    @Test
    void adminVeCitasPorEstadoDeTodos() {
        when(reportRepository.appointmentsByStatus(NOV_1, NOV_30, null)).thenReturn(List.of(
                new ReportRows.AppointmentsByStatus(1, "Programada", "PROGRAMADA", 4),
                new ReportRows.AppointmentsByStatus(3, "Cancelada", "CANCELADA", 2),
                new ReportRows.AppointmentsByStatus(6, "Atendida", "ATENDIDA", 0)));

        Report<ReportRows.AppointmentsByStatus> report = service.appointmentsByStatus(ADMIN, filter("20261101", "20261130", null));

        assertEquals(6, report.getTotal());
        assertEquals(3, report.getItems().size());
        assertEquals("2026-11-01", report.getDesde());
        assertEquals("2026-11-30", report.getHasta());
        assertNull(report.getMedicoId());
    }

    @Test
    void adminFiltraPorVeterinario() {
        when(veterinarianRepository.findById(11)).thenReturn(Optional.of(new VeterinarianInfo()));
        when(reportRepository.attentionsByVeterinarian(NOV_1, NOV_30, 11)).thenReturn(List.of(
                new ReportRows.AttentionsByVeterinarian(11, "Carlos Perez", true, 7, 5)));

        Report<ReportRows.AttentionsByVeterinarian> report =
                service.attentionsByVeterinarian(ADMIN, filter("20261101", "20261130", 11));

        assertEquals(7, report.getTotal());
        assertEquals(11, report.getMedicoId());
    }

    @Test
    void adminConVeterinarioInexistenteFalla() {
        when(veterinarianRepository.findById(99)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.attendedPets(ADMIN, filter("20261101", "20261130", 99)));
        assertEquals("El veterinario no existe", e.getMessage());
    }

    @Test
    void veterinarioVeSoloSusDatosSinEnviarMedicoId() {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));
        when(reportRepository.attendedPets(NOV_1, NOV_30, 11)).thenReturn(List.of(
                new ReportRows.AttendedPet(7, "Firulais", "Labrador", 5, "Ana Gomez", 2, "2026-11-20T14:42:10"),
                new ReportRows.AttendedPet(8, "Luna", "Criollo", 6, "Luis Diaz", 1, "2026-11-10T09:00:00")));

        Report<ReportRows.AttendedPet> report = service.attendedPets(VETERINARIO, filter("20261101", "20261130", null));

        assertEquals(2, report.getTotal());
        assertEquals(11, report.getMedicoId());
    }

    @Test
    void veterinarioNoVeDatosDeOtro() {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));

        assertThrows(AccessDeniedException.class,
                () -> service.appointmentsByStatus(VETERINARIO, filter("20261101", "20261130", 12)));
        verify(reportRepository, never()).appointmentsByStatus(any(), any(), any());
    }

    @Test
    void clienteNoVeReportes() {
        assertThrows(AccessDeniedException.class,
                () -> service.appointmentsByStatus(CLIENTE, filter("20261101", "20261130", null)));
    }

    @Test
    void hastaAntesDeDesdeFalla() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.appointmentsByStatus(ADMIN, filter("20261130", "20261101", null)));
        assertEquals("La fecha hasta no puede ser anterior a la fecha desde", e.getMessage());
    }

    @Test
    void periodoMayorAUnAnoFalla() {
        assertThrows(BusinessException.class,
                () -> service.appointmentsByStatus(ADMIN, filter("20250101", "20260102", null)));
    }

    @Test
    void periodoDeUnAnoBisiestoCompletoEsValido() {
        when(reportRepository.appointmentsByStatus(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31), null))
                .thenReturn(List.of());

        assertEquals(0, service.appointmentsByStatus(ADMIN, filter("20280101", "20281231", null)).getTotal());
    }

    @Test
    void fechaInexistenteFalla() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.appointmentsByStatus(ADMIN, filter("20260230", "20260301", null)));
        assertEquals("La fecha desde no es valida", e.getMessage());
    }
}
