package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.enums.AppointmentStatus;
import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.DataAppointmentDTO;
import com.example.proyectoIntegrador.models.DataCancelledAppointmentDTO;
import com.example.proyectoIntegrador.models.DataVeterinarianAgendaDTO;
import com.example.proyectoIntegrador.models.RescheduleAppointmentDTO;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    // ---------- VET-04: agendar, cancelar y reprogramar ----------

    private static final LocalDate FUTURE = LocalDate.now().plusDays(10);
    private static final String FUTURE_TEXT = FUTURE.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    private static final LocalTime TEN = LocalTime.of(10, 0);

    private static DataAppointmentDTO nuevaCita() {
        DataAppointmentDTO dto = new DataAppointmentDTO();
        dto.setClienteId(5);
        dto.setMedicoId(11);
        dto.setMascotaId(8);
        dto.setDescription("Control");
        dto.setFecha(FUTURE_TEXT);
        dto.setHora("100000");
        return dto;
    }

    private static VeterinarianAgendaItem citaDe(int clienteId, int medicoId, int estadoId, String estado) {
        VeterinarianAgendaItem cita = cita(medicoId, estadoId, estado);
        cita.setClienteId(clienteId);
        return cita;
    }

    private static RescheduleAppointmentDTO reprogramar(String fecha, String hora, Integer medicoId) {
        RescheduleAppointmentDTO dto = new RescheduleAppointmentDTO();
        dto.setFecha(fecha);
        dto.setHora(hora);
        dto.setMedicoId(medicoId);
        return dto;
    }

    @Test
    void agendarAsignaSiempreElEstadoProgramada() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.isMascotaOfClient(8, 5)).thenReturn(true);
        when(agendaCitaRepository.isSlotAvailable(11, FUTURE, TEN, null)).thenReturn(true);
        when(agendaCitaRepository.findEstadoId(AppointmentStatus.PROGRAMADA)).thenReturn(Optional.of(1));
        when(agendaCitaRepository.insertAgendaCita(any(), any(), any(), any(), any(), any(), any())).thenReturn(Optional.of(500));

        service.scheduleAnAppointment(nuevaCita());

        verify(agendaCitaRepository).insertAgendaCita("Control", FUTURE_TEXT, TEN, 1, 5, 11, 8);
    }

    @Test
    void agendarConMascotaDeOtroClienteFalla() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.isMascotaOfClient(8, 5)).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class, () -> service.scheduleAnAppointment(nuevaCita()));
        assertEquals("La mascota no existe, no pertenece al cliente o esta inactiva", e.getMessage());
        verify(agendaCitaRepository, never()).insertAgendaCita(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void agendarEnHorarioOcupadoFalla() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.isMascotaOfClient(8, 5)).thenReturn(true);
        when(agendaCitaRepository.isSlotAvailable(11, FUTURE, TEN, null)).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class, () -> service.scheduleAnAppointment(nuevaCita()));
        assertEquals("El veterinario no tiene disponible ese horario", e.getMessage());
    }

    @Test
    void agendarEnElPasadoFalla() {
        DataAppointmentDTO dto = nuevaCita();
        dto.setFecha("20200101");

        BusinessException e = assertThrows(BusinessException.class, () -> service.scheduleAnAppointment(dto));
        assertEquals("La fecha y hora de la cita ya pasaron", e.getMessage());
    }

    @Test
    void agendarConHoraInexistenteFalla() {
        DataAppointmentDTO dto = nuevaCita();
        dto.setHora("256000");

        BusinessException e = assertThrows(BusinessException.class, () -> service.scheduleAnAppointment(dto));
        assertEquals("La hora no es valida", e.getMessage());
    }

    @Test
    void agendarSinEstadoProgramadaEnBdEsErrorInterno() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.isMascotaOfClient(8, 5)).thenReturn(true);
        when(agendaCitaRepository.isSlotAvailable(11, FUTURE, TEN, null)).thenReturn(true);
        when(agendaCitaRepository.findEstadoId(AppointmentStatus.PROGRAMADA)).thenReturn(Optional.empty());

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.scheduleAnAppointment(nuevaCita()));
        assertFalse(e instanceof BusinessException);
    }

    private static DataCancelledAppointmentDTO cancelar(int clienteId, int agendaId) {
        return new DataCancelledAppointmentDTO(clienteId, agendaId);
    }

    @Test
    void cancelarUsaElEstadoCanceladaPorDescripcion() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(citaDe(5, 11, 2, "Confirmada")));
        when(agendaCitaRepository.findEstadoId(AppointmentStatus.CANCELADA)).thenReturn(Optional.of(7));
        when(agendaCitaRepository.updateEstado(100, 2, 7)).thenReturn(1);

        service.cancelledAppointment(cancelar(5, 100));

        verify(agendaCitaRepository).updateEstado(100, 2, 7);
    }

    @Test
    void cancelarCitaDeOtroClienteNoLaEncuentra() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(citaDe(6, 11, 2, "Confirmada")));

        BusinessException e = assertThrows(BusinessException.class, () -> service.cancelledAppointment(cancelar(5, 100)));
        assertEquals("La cita no existe", e.getMessage());
        verify(agendaCitaRepository, never()).updateEstado(anyInt(), anyInt(), anyInt());
    }

    @Test
    void cancelarCitaAtendidaFalla() {
        when(agendaCitaRepository.validateClient(5)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(citaDe(5, 11, 5, "Atendida")));

        BusinessException e = assertThrows(BusinessException.class, () -> service.cancelledAppointment(cancelar(5, 100)));
        assertEquals("No se puede cancelar una cita en estado 'Atendida'", e.getMessage());
    }

    @Test
    void clienteReprogramaSuCitaYQuedaReprogramada() {
        when(agendaCitaRepository.findAgendaItem(100))
                .thenReturn(Optional.of(citaDe(5, 11, 1, "Programada")), Optional.of(citaDe(5, 11, 4, "Reprogramada")));
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.isSlotAvailable(11, FUTURE, TEN, 100)).thenReturn(true);
        when(agendaCitaRepository.findEstadoId(AppointmentStatus.REPROGRAMADA)).thenReturn(Optional.of(4));
        when(agendaCitaRepository.reschedule(100, 1, FUTURE, TEN, 11, 4)).thenReturn(1);

        VeterinarianAgendaItem result = service.rescheduleAppointment(CLIENTE, 100, reprogramar(FUTURE_TEXT, "100000", null));

        assertEquals("Reprogramada", result.getEstado());
        // Se excluye la propia cita al revisar disponibilidad y se conserva el veterinario
        verify(agendaCitaRepository).reschedule(100, 1, FUTURE, TEN, 11, 4);
    }

    @Test
    void reprogramarConOtroVeterinario() {
        when(agendaCitaRepository.findAgendaItem(100))
                .thenReturn(Optional.of(citaDe(5, 11, 1, "Programada")), Optional.of(citaDe(5, 12, 4, "Reprogramada")));
        when(agendaCitaRepository.validateVeterinarian(12)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.isSlotAvailable(12, FUTURE, TEN, 100)).thenReturn(true);
        when(agendaCitaRepository.findEstadoId(AppointmentStatus.REPROGRAMADA)).thenReturn(Optional.of(4));
        when(agendaCitaRepository.reschedule(100, 1, FUTURE, TEN, 12, 4)).thenReturn(1);

        service.rescheduleAppointment(ADMIN, 100, reprogramar(FUTURE_TEXT, "100000", 12));

        verify(agendaCitaRepository).reschedule(100, 1, FUTURE, TEN, 12, 4);
    }

    @Test
    void clienteNoPuedeReprogramarCitaDeOtroCliente() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(citaDe(6, 11, 1, "Programada")));

        assertThrows(AccessDeniedException.class,
                () -> service.rescheduleAppointment(CLIENTE, 100, reprogramar(FUTURE_TEXT, "100000", null)));
        verify(agendaCitaRepository, never()).reschedule(anyInt(), anyInt(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void veterinarioReprogramaSoloSusCitas() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(citaDe(5, 12, 1, "Programada")));
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));

        assertThrows(AccessDeniedException.class,
                () -> service.rescheduleAppointment(VETERINARIO, 100, reprogramar(FUTURE_TEXT, "100000", null)));
    }

    @Test
    void noSeReprogramaUnaCitaCancelada() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(citaDe(5, 11, 3, "Cancelada")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.rescheduleAppointment(CLIENTE, 100, reprogramar(FUTURE_TEXT, "100000", null)));
        assertEquals("No se puede reprogramar una cita en estado 'Cancelada'", e.getMessage());
    }

    @Test
    void reprogramarAHorarioOcupadoFalla() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(citaDe(5, 11, 1, "Programada")));
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.isSlotAvailable(11, FUTURE, TEN, 100)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.rescheduleAppointment(CLIENTE, 100, reprogramar(FUTURE_TEXT, "100000", null)));
        verify(agendaCitaRepository, never()).reschedule(anyInt(), anyInt(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void reprogramarConVeterinarioInactivoFalla() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(citaDe(5, 11, 1, "Programada")));
        when(agendaCitaRepository.validateVeterinarian(12)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.rescheduleAppointment(CLIENTE, 100, reprogramar(FUTURE_TEXT, "100000", 12)));
        assertEquals("El veterinario no existe o esta inactivo", e.getMessage());
    }

    @Test
    void reprogramarAlPasadoFalla() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(citaDe(5, 11, 1, "Programada")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.rescheduleAppointment(CLIENTE, 100, reprogramar("20200101", "100000", null)));
        assertEquals("La fecha y hora de la cita ya pasaron", e.getMessage());
    }

    @Test
    void reprogramarSiOtraPersonaCambioLaCitaNoSobrescribe() {
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(citaDe(5, 11, 1, "Programada")));
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.of(true));
        when(agendaCitaRepository.isSlotAvailable(11, FUTURE, TEN, 100)).thenReturn(true);
        when(agendaCitaRepository.findEstadoId(AppointmentStatus.REPROGRAMADA)).thenReturn(Optional.of(4));
        when(agendaCitaRepository.reschedule(100, 1, FUTURE, TEN, 11, 4)).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.rescheduleAppointment(CLIENTE, 100, reprogramar(FUTURE_TEXT, "100000", null)));
    }

    @Test
    void fechaInexistenteEsErrorDeNegocio() {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.getVeterinarianAgenda(VETERINARIO, agenda("20260230", null, null)));
        assertEquals("La fecha no es valida", e.getMessage());
    }
}
