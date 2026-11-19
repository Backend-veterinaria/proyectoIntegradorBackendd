package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.ScheduleModels.Availability;
import com.example.proyectoIntegrador.models.ScheduleModels.TimeSlotRange;
import com.example.proyectoIntegrador.models.ScheduleModels.WeekSchedule;
import com.example.proyectoIntegrador.models.ScheduleModels.WeekScheduleDTO;
import com.example.proyectoIntegrador.models.VeterinarianInfo;
import com.example.proyectoIntegrador.repository.AgendaCitaRepository;
import com.example.proyectoIntegrador.repository.ScheduleRepository;
import com.example.proyectoIntegrador.repository.ScheduleRepository.OwnedRange;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.services.Impl.ScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Horarios de atención y parámetros (CIT-08).
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    /** Un día futuro fijo: así "ya pasó" nunca afecta. */
    private static final LocalDate DIA = LocalDate.now().plusDays(7);
    private static final String DIA_TEXT = DIA.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    private static final int DOW = DIA.getDayOfWeek().getValue();

    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private AgendaCitaRepository agendaCitaRepository;
    @Mock
    private VeterinarianRepository veterinarianRepository;

    private ScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScheduleServiceImpl(scheduleRepository, agendaCitaRepository, veterinarianRepository);
    }

    private static OwnedRange general(String ini, String fin) {
        return new OwnedRange(null, new TimeSlotRange(DOW, ini, fin));
    }

    private static OwnedRange propia(String ini, String fin) {
        return new OwnedRange(11, new TimeSlotRange(DOW, ini, fin));
    }

    private void givenDuration(String value) {
        when(scheduleRepository.findParameter("duracion_cita_minutos")).thenReturn(Optional.ofNullable(value));
    }

    // ---------- Duración ----------

    @Test
    void duracionSaleDeLaBaseDeDatos() {
        givenDuration("45");
        assertEquals(45, service.appointmentMinutes());
    }

    @Test
    void duracionInvalidaOAusenteUsa30() {
        givenDuration("abc");
        assertEquals(30, service.appointmentMinutes());
        givenDuration(null);
        assertEquals(30, service.appointmentMinutes());
        givenDuration("1000");
        assertEquals(30, service.appointmentMinutes());
    }

    // ---------- Horas libres ----------

    @Test
    void horasLibresSaltanLasOcupadas() {
        givenDuration("30");
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.of(true));
        when(scheduleRepository.hasAnySchedule()).thenReturn(true);
        when(scheduleRepository.findDay(11, DOW)).thenReturn(List.of(general("08:00", "10:00"), general("14:00", "15:00")));
        when(agendaCitaRepository.findBusyTimes(11, DIA)).thenReturn(List.of(LocalTime.of(9, 0)));

        Availability a = service.availability(11, DIA_TEXT);

        assertEquals(List.of("08:00", "08:30", "09:30", "14:00", "14:30"), a.getHorarios());
        assertEquals(30, a.getDuracionMinutos());
    }

    @Test
    void citaFueraDeLaRejillaBloqueaLasDosFranjasVecinas() {
        givenDuration("30");
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.of(true));
        when(scheduleRepository.hasAnySchedule()).thenReturn(true);
        when(scheduleRepository.findDay(11, DOW)).thenReturn(List.of(general("08:00", "10:00")));
        when(agendaCitaRepository.findBusyTimes(11, DIA)).thenReturn(List.of(LocalTime.of(8, 45)));

        // 08:30 y 09:00 quedan a menos de 30 minutos de la cita de las 08:45
        assertEquals(List.of("08:00", "09:30"), service.availability(11, DIA_TEXT).getHorarios());
    }

    @Test
    void horarioPropioDelVeterinarioReemplazaAlGeneral() {
        givenDuration("60");
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.of(true));
        when(scheduleRepository.hasAnySchedule()).thenReturn(true);
        when(scheduleRepository.findDay(11, DOW)).thenReturn(List.of(general("08:00", "12:00"), propia("14:00", "16:00")));
        when(agendaCitaRepository.findBusyTimes(11, DIA)).thenReturn(List.of());

        assertEquals(List.of("14:00", "15:00"), service.availability(11, DIA_TEXT).getHorarios());
    }

    @Test
    void diaSinFranjasEstaCerrado() {
        givenDuration("30");
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.of(true));
        when(scheduleRepository.hasAnySchedule()).thenReturn(true);
        when(scheduleRepository.findDay(11, DOW)).thenReturn(List.of());
        when(agendaCitaRepository.findBusyTimes(11, DIA)).thenReturn(List.of());

        assertTrue(service.availability(11, DIA_TEXT).getHorarios().isEmpty());
    }

    @Test
    void sinHorariosConfiguradosNoHayDisponibilidad() {
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.of(true));
        when(scheduleRepository.hasAnySchedule()).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class, () -> service.availability(11, DIA_TEXT));
        assertEquals("No hay horarios de atencion configurados", e.getMessage());
    }

    @Test
    void veterinarioInactivoNoTieneDisponibilidad() {
        when(agendaCitaRepository.validateVeterinarian(11)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.availability(11, DIA_TEXT));
    }

    // ---------- Validación al agendar ----------

    @Test
    void horaDentroDelHorarioSeAcepta() {
        when(scheduleRepository.hasAnySchedule()).thenReturn(true);
        when(scheduleRepository.findDay(11, DOW)).thenReturn(List.of(general("08:00", "12:00")));

        assertDoesNotThrow(() -> service.requireWithinSchedule(11, DIA, LocalTime.of(11, 30), 30));
    }

    @Test
    void citaQueTerminaDespuesDelCierreSeRechaza() {
        when(scheduleRepository.hasAnySchedule()).thenReturn(true);
        when(scheduleRepository.findDay(11, DOW)).thenReturn(List.of(general("08:00", "12:00")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.requireWithinSchedule(11, DIA, LocalTime.of(11, 45), 30));
        assertEquals("El veterinario no atiende en ese horario", e.getMessage());
    }

    @Test
    void sinHorariosConfiguradosNoSeValida() {
        when(scheduleRepository.hasAnySchedule()).thenReturn(false);

        assertDoesNotThrow(() -> service.requireWithinSchedule(11, DIA, LocalTime.of(3, 0), 30));
        verify(scheduleRepository, never()).findDay(anyInt(), anyInt());
    }

    // ---------- Administración ----------

    @Test
    void veterinarioSinHorarioPropioUsaElGeneral() {
        when(veterinarianRepository.findById(11)).thenReturn(Optional.of(new VeterinarianInfo()));
        when(scheduleRepository.findWeek(11)).thenReturn(List.of());
        when(scheduleRepository.findWeek(null)).thenReturn(List.of(new TimeSlotRange(1, "08:00", "12:00")));

        WeekSchedule w = service.getWeek(11);

        assertTrue(w.isUsaHorarioGeneral());
        assertEquals(1, w.getFranjas().size());
    }

    @Test
    void franjasQueSeCruzanSeRechazan() {
        WeekScheduleDTO dto = new WeekScheduleDTO();
        dto.setFranjas(List.of(new TimeSlotRange(1, "08:00", "12:00"), new TimeSlotRange(1, "11:00", "13:00")));

        assertThrows(BusinessException.class, () -> service.replaceWeek(null, dto));
        verify(scheduleRepository, never()).replaceWeek(any(), any());
    }

    @Test
    void franjaConFinAntesDelInicioSeRechaza() {
        WeekScheduleDTO dto = new WeekScheduleDTO();
        dto.setFranjas(List.of(new TimeSlotRange(2, "12:00", "08:00")));

        assertThrows(BusinessException.class, () -> service.replaceWeek(null, dto));
    }

    @Test
    void duracionFueraDeRangoNoSeGuarda() {
        assertThrows(BusinessException.class, () -> service.updateParameter("duracion_cita_minutos", "2"));
        verify(scheduleRepository, never()).updateParameter(any(), any());
    }

    @Test
    void parametroInexistenteEsErrorDeNegocio() {
        when(scheduleRepository.updateParameter("no_existe", "x")).thenReturn(0);

        BusinessException e = assertThrows(BusinessException.class, () -> service.updateParameter("no_existe", "x"));
        assertEquals("El parametro no existe", e.getMessage());
    }
}
