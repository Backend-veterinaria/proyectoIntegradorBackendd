package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.DataVeterinarianAgendaDTO;
import com.example.proyectoIntegrador.models.VeterinarianAgendaItem;
import com.example.proyectoIntegrador.models.VeterinarianInfo;
import com.example.proyectoIntegrador.repository.AgendaCitaRepository;
import com.example.proyectoIntegrador.repository.ProyectoRepository;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.Impl.AgendaServiceImpl;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaServiceImplTest {

    private static final AuthenticatedUser VETERINARIO = new AuthenticatedUser(40, "dr.perez", Role.VETERINARIO);
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, "admin", Role.ADMIN);
    private static final AuthenticatedUser CLIENTE = new AuthenticatedUser(5, "ana", Role.CLIENTE);

    @Mock
    private AgendaCitaRepository agendaCitaRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private VeterinarianRepository veterinarianRepository;

    private AgendaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AgendaServiceImpl(agendaCitaRepository, proyectoRepository, notificationService, veterinarianRepository);
    }

    private static DataVeterinarianAgendaDTO agenda(String fecha, Integer medicoId, Integer estadoId) {
        DataVeterinarianAgendaDTO dto = new DataVeterinarianAgendaDTO();
        dto.setFecha(fecha);
        dto.setMedicoId(medicoId);
        dto.setEstadoId(estadoId);
        return dto;
    }

    @Test
    void veterinarioVeSuAgendaSinEnviarMedicoId() {
        VeterinarianAgendaItem item = new VeterinarianAgendaItem();
        item.setAgendaId(100);
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));
        when(agendaCitaRepository.getAgendaByVeterinarian(11, LocalDate.of(2026, 11, 20), null)).thenReturn(List.of(item));

        List<VeterinarianAgendaItem> result = service.getVeterinarianAgenda(VETERINARIO, agenda("20261120", null, null));

        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getAgendaId());
    }

    @Test
    void veterinarioPuedeEnviarSuPropioMedicoIdYFiltrarPorEstado() {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));
        when(agendaCitaRepository.getAgendaByVeterinarian(11, LocalDate.of(2026, 11, 20), 2)).thenReturn(List.of());

        service.getVeterinarianAgenda(VETERINARIO, agenda("20261120", 11, 2));

        verify(agendaCitaRepository).getAgendaByVeterinarian(11, LocalDate.of(2026, 11, 20), 2);
    }

    @Test
    void veterinarioNoPuedeVerLaAgendaDeOtro() {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));

        assertThrows(AccessDeniedException.class,
                () -> service.getVeterinarianAgenda(VETERINARIO, agenda("20261120", 12, null)));
        verify(agendaCitaRepository, never()).getAgendaByVeterinarian(anyInt(), any(), any());
    }

    @Test
    void veterinarioSinVinculoConMedicoEsErrorDeNegocio() {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.getVeterinarianAgenda(VETERINARIO, agenda("20261120", null, null)));
        assertEquals("Su usuario no esta vinculado a un veterinario", e.getMessage());
    }

    @Test
    void adminVeLaAgendaDeCualquierVeterinario() {
        when(veterinarianRepository.findById(12)).thenReturn(Optional.of(new VeterinarianInfo()));
        when(agendaCitaRepository.getAgendaByVeterinarian(12, LocalDate.of(2026, 11, 20), null)).thenReturn(List.of());

        service.getVeterinarianAgenda(ADMIN, agenda("20261120", 12, null));

        verify(agendaCitaRepository).getAgendaByVeterinarian(12, LocalDate.of(2026, 11, 20), null);
    }

    @Test
    void adminDebeIndicarMedicoId() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.getVeterinarianAgenda(ADMIN, agenda("20261120", null, null)));
        assertEquals("Debe indicar el veterinario (medicoId)", e.getMessage());
    }

    @Test
    void adminConVeterinarioInexistenteEsErrorDeNegocio() {
        when(veterinarianRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> service.getVeterinarianAgenda(ADMIN, agenda("20261120", 99, null)));
        verify(agendaCitaRepository, never()).getAgendaByVeterinarian(anyInt(), any(), any());
    }

    @Test
    void clienteNoPuedeVerAgendasDeVeterinarios() {
        assertThrows(AccessDeniedException.class,
                () -> service.getVeterinarianAgenda(CLIENTE, agenda("20261120", 11, null)));
    }

    // ---------- VET-03: cambio de estado ----------

    private static VeterinarianAgendaItem cita(int medicoId, int estadoId, String estado) {
        VeterinarianAgendaItem cita = new VeterinarianAgendaItem();
        cita.setAgendaId(100);
        cita.setMedicoId(medicoId);
        cita.setEstadoId(estadoId);
        cita.setEstado(estado);
        return cita;
    }

    @Test
    void veterinarioMarcaSuCitaComoAtendida() {
        when(agendaCitaRepository.findAgendaItem(100))
                .thenReturn(Optional.of(cita(11, 2, "Confirmada")), Optional.of(cita(11, 5, "Atendida")));
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));
        when(agendaCitaRepository.getEstadoDescription(5)).thenReturn(Optional.of("Atendida"));
        when(agendaCitaRepository.updateEstado(100, 2, 5)).thenReturn(1);

        VeterinarianAgendaItem result = service.changeAppointmentStatus(VETERINARIO, 100, 5);

        assertEquals("Atendida", result.getEstado());
        verify(agendaCitaRepository).updateEstado(100, 2, 5);
    }

    @Test
    void veterinarioNoPuedeCambiarCitasDeOtroVeterinario() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita(12, 2, "Confirmada")));
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));

        assertThrows(AccessDeniedException.class, () -> service.changeAppointmentStatus(VETERINARIO, 100, 5));
        verify(agendaCitaRepository, never()).updateEstado(anyInt(), anyInt(), anyInt());
    }

    @Test
    void adminPuedeCambiarCualquierCita() {
        when(agendaCitaRepository.findAgendaItem(100))
                .thenReturn(Optional.of(cita(12, 1, "Programada")), Optional.of(cita(12, 6, "No asistio")));
        when(agendaCitaRepository.getEstadoDescription(6)).thenReturn(Optional.of("No asistio"));
        when(agendaCitaRepository.updateEstado(100, 1, 6)).thenReturn(1);

        service.changeAppointmentStatus(ADMIN, 100, 6);

        verify(agendaCitaRepository).updateEstado(100, 1, 6);
    }

    @Test
    void clienteNoPuedeCambiarEstados() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita(11, 1, "Programada")));

        assertThrows(AccessDeniedException.class, () -> service.changeAppointmentStatus(CLIENTE, 100, 5));
    }

    @Test
    void citaCanceladaNoCambiaDeEstado() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita(12, 3, "Cancelada")));
        when(agendaCitaRepository.getEstadoDescription(5)).thenReturn(Optional.of("Atendida"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changeAppointmentStatus(ADMIN, 100, 5));
        assertEquals("No se puede cambiar una cita en estado 'Cancelada' a 'Atendida'", e.getMessage());
        verify(agendaCitaRepository, never()).updateEstado(anyInt(), anyInt(), anyInt());
    }

    @Test
    void citaInexistenteEsErrorDeNegocio() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changeAppointmentStatus(ADMIN, 100, 5));
        assertEquals("La cita no existe", e.getMessage());
    }

    @Test
    void estadoInexistenteEsErrorDeNegocio() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita(12, 1, "Programada")));
        when(agendaCitaRepository.getEstadoDescription(99)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.changeAppointmentStatus(ADMIN, 100, 99));
        assertEquals("El estado no existe", e.getMessage());
    }

    @Test
    void siOtraPersonaCambioElEstadoAntesNoSeSobrescribe() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita(12, 1, "Programada")));
        when(agendaCitaRepository.getEstadoDescription(5)).thenReturn(Optional.of("Atendida"));
        when(agendaCitaRepository.updateEstado(100, 1, 5)).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.changeAppointmentStatus(ADMIN, 100, 5));
    }

    @Test
    void fechaInexistenteEsErrorDeNegocio() {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.getVeterinarianAgenda(VETERINARIO, agenda("20260230", null, null)));
        assertEquals("La fecha no es valida", e.getMessage());
    }
}
