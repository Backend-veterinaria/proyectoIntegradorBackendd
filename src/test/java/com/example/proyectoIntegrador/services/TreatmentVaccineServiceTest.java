package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.enums.TreatmentStatus;
import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.ClinicalAttention;
import com.example.proyectoIntegrador.models.InfoMascota;
import com.example.proyectoIntegrador.models.Treatment;
import com.example.proyectoIntegrador.models.TreatmentCreateDTO;
import com.example.proyectoIntegrador.models.Vaccine;
import com.example.proyectoIntegrador.models.VaccineCreateDTO;
import com.example.proyectoIntegrador.repository.AgendaCitaRepository;
import com.example.proyectoIntegrador.repository.ClinicalHistoryRepository;
import com.example.proyectoIntegrador.repository.PetsRepository;
import com.example.proyectoIntegrador.repository.TreatmentVaccineRepository;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.security.Role;
import com.example.proyectoIntegrador.services.Impl.ClinicalHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tratamientos (HC-04) y vacunas (HC-05).
 */
@ExtendWith(MockitoExtension.class)
class TreatmentVaccineServiceTest {

    private static final AuthenticatedUser VETERINARIO = new AuthenticatedUser(40, "dr.perez", Role.VETERINARIO);
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1, "admin", Role.ADMIN);
    private static final AuthenticatedUser CLIENTE = new AuthenticatedUser(5, "ana", Role.CLIENTE);
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Mock
    private ClinicalHistoryRepository clinicalHistoryRepository;
    @Mock
    private AgendaCitaRepository agendaCitaRepository;
    @Mock
    private VeterinarianRepository veterinarianRepository;
    @Mock
    private PetsRepository petsRepository;
    @Mock
    private TreatmentVaccineRepository repo;

    private ClinicalHistoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClinicalHistoryServiceImpl(clinicalHistoryRepository, agendaCitaRepository, veterinarianRepository,
                petsRepository, repo);
    }

    private void givenVetAndPet(boolean petActive) {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));
        InfoMascota m = new InfoMascota();
        m.setIdMascota(7);
        m.setClienteId(5);
        m.setActivo(petActive);
        when(petsRepository.findById(7)).thenReturn(Optional.of(m));
    }

    private static TreatmentCreateDTO treatment(String inicio, String fin) {
        TreatmentCreateDTO dto = new TreatmentCreateDTO();
        dto.setMascotaId(7);
        dto.setMedicamento(" Amoxicilina ");
        dto.setDosis("250 mg");
        dto.setFrecuencia("Cada 12 horas");
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
        return dto;
    }

    private static VaccineCreateDTO vaccine(LocalDate aplicacion, LocalDate proxima) {
        VaccineCreateDTO dto = new VaccineCreateDTO();
        dto.setMascotaId(7);
        dto.setNombre("Rabia");
        dto.setFechaAplicacion(aplicacion.format(YMD));
        dto.setProximaDosis(proxima == null ? null : proxima.format(YMD));
        return dto;
    }

    private static Treatment treatmentWithStatus(TreatmentStatus status) {
        Treatment t = new Treatment();
        t.setTratamientoId(300);
        t.setEstado(status.name());
        return t;
    }

    // ---------- Tratamientos (HC-04) ----------

    @Test
    void veterinarioRegistraTratamiento() {
        givenVetAndPet(true);
        when(repo.insertTreatment(eq(7), isNull(), eq(11), eq("Amoxicilina"), eq("250 mg"), eq("Cada 12 horas"),
                isNull(), eq(LocalDate.of(2026, 11, 20)), eq(LocalDate.of(2026, 11, 27)))).thenReturn(300);
        when(repo.findTreatment(300)).thenReturn(Optional.of(treatmentWithStatus(TreatmentStatus.EN_CURSO)));

        Treatment result = service.registerTreatment(VETERINARIO, treatment("20261120", "20261127"));

        assertEquals(300, result.getTratamientoId());
    }

    @Test
    void tratamientoConFinAntesDelInicioFalla() {
        givenVetAndPet(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.registerTreatment(VETERINARIO, treatment("20261120", "20261119")));
        assertEquals("La fecha de fin no puede ser anterior a la fecha de inicio", e.getMessage());
    }

    @Test
    void tratamientoConFechaInexistenteFalla() {
        givenVetAndPet(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.registerTreatment(VETERINARIO, treatment("20260230", null)));
        assertEquals("La fecha de inicio no es valida", e.getMessage());
    }

    @Test
    void tratamientoAMascotaInactivaFalla() {
        givenVetAndPet(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.registerTreatment(VETERINARIO, treatment("20261120", null)));
        assertEquals("La mascota esta inactiva", e.getMessage());
        verify(repo, never()).insertTreatment(anyInt(), any(), anyInt(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void tratamientoConAtencionDeOtraMascotaFalla() {
        givenVetAndPet(true);
        ClinicalAttention otra = new ClinicalAttention();
        otra.setMascotaId(8);
        when(clinicalHistoryRepository.findById(500)).thenReturn(Optional.of(otra));
        TreatmentCreateDTO dto = treatment("20261120", null);
        dto.setAtencionId(500);

        BusinessException e = assertThrows(BusinessException.class, () -> service.registerTreatment(VETERINARIO, dto));
        assertEquals("La atencion no existe o no es de esta mascota", e.getMessage());
    }

    @Test
    void adminNoRegistraTratamientos() {
        assertThrows(AccessDeniedException.class, () -> service.registerTreatment(ADMIN, treatment("20261120", null)));
        verify(petsRepository, never()).findById(anyInt());
    }

    @Test
    void veterinarioFinalizaTratamientoEnCurso() {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));
        when(repo.findTreatment(300)).thenReturn(Optional.of(treatmentWithStatus(TreatmentStatus.EN_CURSO)),
                Optional.of(treatmentWithStatus(TreatmentStatus.FINALIZADO)));
        when(repo.updateTreatmentStatus(300, TreatmentStatus.EN_CURSO, TreatmentStatus.FINALIZADO, LocalDate.now()))
                .thenReturn(1);

        Treatment result = service.changeTreatmentStatus(VETERINARIO, 300, TreatmentStatus.FINALIZADO);

        assertEquals("FINALIZADO", result.getEstado());
    }

    @Test
    void tratamientoFinalizadoNoCambiaDeEstado() {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));
        when(repo.findTreatment(300)).thenReturn(Optional.of(treatmentWithStatus(TreatmentStatus.FINALIZADO)));

        assertThrows(BusinessException.class,
                () -> service.changeTreatmentStatus(VETERINARIO, 300, TreatmentStatus.SUSPENDIDO));
        verify(repo, never()).updateTreatmentStatus(anyInt(), any(), any(), any());
    }

    @Test
    void cambioConcurrenteDeTratamientoFalla() {
        when(veterinarianRepository.findMedicoIdByClienteId(40)).thenReturn(Optional.of(11));
        when(repo.findTreatment(300)).thenReturn(Optional.of(treatmentWithStatus(TreatmentStatus.EN_CURSO)));
        when(repo.updateTreatmentStatus(anyInt(), any(), any(), any())).thenReturn(0);

        assertThrows(BusinessException.class,
                () -> service.changeTreatmentStatus(VETERINARIO, 300, TreatmentStatus.SUSPENDIDO));
    }

    // ---------- Vacunas (HC-05) ----------

    @Test
    void veterinarioRegistraVacuna() {
        givenVetAndPet(true);
        LocalDate hoy = LocalDate.now();
        Vaccine saved = new Vaccine();
        saved.setVacunaId(900);
        when(repo.insertVaccine(7, null, 11, "Rabia", null, hoy, hoy.plusYears(1), null)).thenReturn(900);
        when(repo.findVaccine(900)).thenReturn(Optional.of(saved));

        assertEquals(900, service.registerVaccine(VETERINARIO, vaccine(hoy, hoy.plusYears(1))).getVacunaId());
    }

    @Test
    void vacunaConFechaFuturaFalla() {
        givenVetAndPet(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.registerVaccine(VETERINARIO, vaccine(LocalDate.now().plusDays(1), null)));
        assertEquals("La fecha de aplicacion no puede ser futura", e.getMessage());
    }

    @Test
    void proximaDosisDebeSerPosterior() {
        givenVetAndPet(true);
        LocalDate hoy = LocalDate.now();

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.registerVaccine(VETERINARIO, vaccine(hoy, hoy)));
        assertEquals("La proxima dosis debe ser posterior a la fecha de aplicacion", e.getMessage());
    }

    @Test
    void clienteNoRegistraVacunas() {
        assertThrows(AccessDeniedException.class,
                () -> service.registerVaccine(CLIENTE, vaccine(LocalDate.now(), null)));
    }

    @Test
    void personalVeTodasLasVacunasPendientes() {
        when(repo.findUpcomingVaccines(LocalDate.now().plusDays(30), null)).thenReturn(List.of(new Vaccine()));

        assertEquals(1, service.getUpcomingVaccines(VETERINARIO, 30, null).size());
    }

    @Test
    void clienteSoloVeLasVacunasDeSusMascotas() {
        when(repo.findUpcomingVaccines(LocalDate.now().plusDays(15), 5)).thenReturn(List.of());

        service.getUpcomingVaccines(CLIENTE, 15, null);

        verify(repo).findUpcomingVaccines(LocalDate.now().plusDays(15), 5);
    }

    @Test
    void clienteNoVeVacunasDeOtroCliente() {
        assertThrows(AccessDeniedException.class, () -> service.getUpcomingVaccines(CLIENTE, 30, 6));
        verify(repo, never()).findUpcomingVaccines(any(), any());
    }

    @Test
    void diasFueraDeRangoFalla() {
        assertThrows(BusinessException.class, () -> service.getUpcomingVaccines(ADMIN, 400, null));
        assertThrows(BusinessException.class, () -> service.getUpcomingVaccines(ADMIN, -1, null));
    }
}
