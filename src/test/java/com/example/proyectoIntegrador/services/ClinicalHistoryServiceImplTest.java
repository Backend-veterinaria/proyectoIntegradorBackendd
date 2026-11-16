package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.enums.AppointmentStatus;
import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.ClinicalAttention;
import com.example.proyectoIntegrador.models.ClinicalAttentionCreateDTO;
import com.example.proyectoIntegrador.models.ClinicalHistory;
import com.example.proyectoIntegrador.models.InfoMascota;
import com.example.proyectoIntegrador.models.VeterinarianAgendaItem;
import com.example.proyectoIntegrador.repository.AgendaCitaRepository;
import com.example.proyectoIntegrador.repository.ClinicalHistoryRepository;
import com.example.proyectoIntegrador.repository.PetsRepository;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.Impl.ClinicalHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalHistoryServiceImplTest {

    private static final AuthenticatedUser VETERINARIO = new AuthenticatedUser(40, "dr.perez", Role.VETERINARIO);
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, "admin", Role.ADMIN);
    private static final AuthenticatedUser CLIENTE = new AuthenticatedUser(5, "ana", Role.CLIENTE);

    @Mock
    private ClinicalHistoryRepository clinicalHistoryRepository;
    @Mock
    private AgendaCitaRepository agendaCitaRepository;
    @Mock
    private VeterinarianRepository veterinarianRepository;
    @Mock
    private PetsRepository petsRepository;

    private ClinicalHistoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClinicalHistoryServiceImpl(clinicalHistoryRepository, agendaCitaRepository, veterinarianRepository, petsRepository);
    }

    private static VeterinarianAgendaItem cita(String estado, int estadoId, int medicoId, LocalDate fecha) {
        VeterinarianAgendaItem c = new VeterinarianAgendaItem();
        c.setAgendaId(100);
        c.setMedicoId(medicoId);
        c.setMascotaId(7);
        c.setClienteId(5);
        c.setEstado(estado);
        c.setEstadoId(estadoId);
        c.setFecha(fecha.toString());
        return c;
    }

    private static ClinicalAttentionCreateDTO attention() {
        ClinicalAttentionCreateDTO dto = new ClinicalAttentionCreateDTO();
        dto.setAgendaId(100);
        dto.setDiagnostico("  Otitis externa ");
        dto.setProcedimientos("Limpieza de oido");
        dto.setObservaciones("   ");
        return dto;
    }

    private void givenVet() {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));
    }

    private static ClinicalAttention saved() {
        ClinicalAttention a = new ClinicalAttention();
        a.setAtencionId(500);
        a.setAgendaId(100);
        a.setClienteId(5);
        return a;
    }

    @Test
    void veterinarioRegistraAtencionYLaCitaPasaAAtendida() {
        givenVet();
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita("Confirmada", 5, 11, LocalDate.now())));
        when(clinicalHistoryRepository.existsByAgendaId(100)).thenReturn(false);
        when(agendaCitaRepository.findEstadoId(AppointmentStatus.ATENDIDA)).thenReturn(Optional.of(6));
        when(agendaCitaRepository.updateEstado(100, 5, 6)).thenReturn(1);
        when(clinicalHistoryRepository.insert(any(), eq(7), eq(11))).thenReturn(500);
        when(clinicalHistoryRepository.findById(500)).thenReturn(Optional.of(saved()));

        ClinicalAttention result = service.registerAttention(VETERINARIO, attention());

        assertEquals(500, result.getAtencionId());
        ArgumentCaptor<ClinicalAttentionCreateDTO> captor = ArgumentCaptor.forClass(ClinicalAttentionCreateDTO.class);
        verify(clinicalHistoryRepository).insert(captor.capture(), eq(7), eq(11));
        assertEquals("Otitis externa", captor.getValue().getDiagnostico());
        assertNull(captor.getValue().getObservaciones());
    }

    @Test
    void citaYaAtendidaSinAtencionSeRegistraSinCambiarEstado() {
        givenVet();
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita("Atendida", 6, 11, LocalDate.now().minusDays(1))));
        when(clinicalHistoryRepository.existsByAgendaId(100)).thenReturn(false);
        when(clinicalHistoryRepository.insert(any(), eq(7), eq(11))).thenReturn(500);
        when(clinicalHistoryRepository.findById(500)).thenReturn(Optional.of(saved()));

        service.registerAttention(VETERINARIO, attention());

        verify(agendaCitaRepository, never()).updateEstado(anyInt(), anyInt(), anyInt());
    }

    @Test
    void citaConAtencionRegistradaFalla() {
        givenVet();
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita("Atendida", 6, 11, LocalDate.now())));
        when(clinicalHistoryRepository.existsByAgendaId(100)).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class, () -> service.registerAttention(VETERINARIO, attention()));
        assertEquals("La cita ya tiene una atencion registrada", e.getMessage());
        verify(clinicalHistoryRepository, never()).insert(any(), anyInt(), anyInt());
    }

    @Test
    void registroSimultaneoDuplicadoEsErrorDeNegocio() {
        givenVet();
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita("Atendida", 6, 11, LocalDate.now())));
        when(clinicalHistoryRepository.existsByAgendaId(100)).thenReturn(false);
        when(clinicalHistoryRepository.insert(any(), eq(7), eq(11))).thenThrow(new DuplicateKeyException("ux"));

        BusinessException e = assertThrows(BusinessException.class, () -> service.registerAttention(VETERINARIO, attention()));
        assertEquals("La cita ya tiene una atencion registrada", e.getMessage());
    }

    @Test
    void citaCanceladaNoAdmiteAtencion() {
        givenVet();
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita("Cancelada", 3, 11, LocalDate.now())));
        when(clinicalHistoryRepository.existsByAgendaId(100)).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class, () -> service.registerAttention(VETERINARIO, attention()));
        assertEquals("No se puede registrar la atencion de una cita en estado 'Cancelada'", e.getMessage());
        verify(clinicalHistoryRepository, never()).insert(any(), anyInt(), anyInt());
    }

    @Test
    void citaFuturaNoAdmiteAtencion() {
        givenVet();
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita("Programada", 1, 11, LocalDate.now().plusDays(1))));

        BusinessException e = assertThrows(BusinessException.class, () -> service.registerAttention(VETERINARIO, attention()));
        assertEquals("No se puede registrar la atencion antes del dia de la cita", e.getMessage());
    }

    @Test
    void cambioDeEstadoConcurrenteFalla() {
        givenVet();
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita("Programada", 1, 11, LocalDate.now())));
        when(clinicalHistoryRepository.existsByAgendaId(100)).thenReturn(false);
        when(agendaCitaRepository.findEstadoId(AppointmentStatus.ATENDIDA)).thenReturn(Optional.of(6));
        when(agendaCitaRepository.updateEstado(100, 1, 6)).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.registerAttention(VETERINARIO, attention()));
        verify(clinicalHistoryRepository, never()).insert(any(), anyInt(), anyInt());
    }

    @Test
    void otroVeterinarioNoPuedeRegistrar() {
        givenVet();
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.of(cita("Programada", 1, 12, LocalDate.now())));

        assertThrows(AccessDeniedException.class, () -> service.registerAttention(VETERINARIO, attention()));
        verify(clinicalHistoryRepository, never()).insert(any(), anyInt(), anyInt());
    }

    @Test
    void adminNoRegistraAtenciones() {
        assertThrows(AccessDeniedException.class, () -> service.registerAttention(ADMIN, attention()));
        verify(agendaCitaRepository, never()).findAgendaItem(anyInt());
    }

    @Test
    void veterinarioSinVinculoFalla() {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.registerAttention(VETERINARIO, attention()));
        assertEquals("Su usuario no esta vinculado a un veterinario", e.getMessage());
    }

    @Test
    void citaInexistenteFalla() {
        givenVet();
        when(agendaCitaRepository.findAgendaItem(100)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.registerAttention(VETERINARIO, attention()));
        assertEquals("La cita no existe", e.getMessage());
    }

    @Test
    void personalConsultaAtencion() {
        when(clinicalHistoryRepository.findById(500)).thenReturn(Optional.of(saved()));

        assertEquals(500, service.getAttention(ADMIN, 500).getAtencionId());
    }

    @Test
    void duenoConsultaAtencionDeSuMascota() {
        when(clinicalHistoryRepository.findById(500)).thenReturn(Optional.of(saved()));

        assertEquals(500, service.getAttention(CLIENTE, 500).getAtencionId());
    }

    @Test
    void clienteNoConsultaAtencionDeMascotaAjena() {
        ClinicalAttention ajena = saved();
        ajena.setClienteId(6);
        when(clinicalHistoryRepository.findById(500)).thenReturn(Optional.of(ajena));

        assertThrows(AccessDeniedException.class, () -> service.getAttention(CLIENTE, 500));
    }

    private static InfoMascota mascota(int clienteId) {
        InfoMascota m = new InfoMascota();
        m.setIdMascota(7);
        m.setClienteId(clienteId);
        return m;
    }

    @Test
    void duenoConsultaHistoriaDeSuMascota() {
        when(petsRepository.findById(7)).thenReturn(Optional.of(mascota(5)));
        when(clinicalHistoryRepository.findByMascotaId(7)).thenReturn(List.of(saved()));

        ClinicalHistory history = service.getPetHistory(CLIENTE, 7);

        assertEquals(7, history.getMascota().getIdMascota());
        assertEquals(1, history.getAtenciones().size());
    }

    @Test
    void personalConsultaHistoriaDeCualquierMascota() {
        when(petsRepository.findById(7)).thenReturn(Optional.of(mascota(6)));
        when(clinicalHistoryRepository.findByMascotaId(7)).thenReturn(List.of());

        assertEquals(0, service.getPetHistory(VETERINARIO, 7).getAtenciones().size());
    }

    @Test
    void clienteNoConsultaHistoriaDeMascotaAjena() {
        when(petsRepository.findById(7)).thenReturn(Optional.of(mascota(6)));

        assertThrows(AccessDeniedException.class, () -> service.getPetHistory(CLIENTE, 7));
        verify(clinicalHistoryRepository, never()).findByMascotaId(anyInt());
    }

    @Test
    void historiaDeMascotaInexistenteEsErrorDeNegocio() {
        when(petsRepository.findById(7)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.getPetHistory(ADMIN, 7));
        assertEquals("La mascota no existe", e.getMessage());
    }

    @Test
    void atencionInexistenteEsErrorDeNegocio() {
        when(clinicalHistoryRepository.findById(500)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.getAttention(VETERINARIO, 500));
        assertEquals("La atencion no existe", e.getMessage());
    }
}
