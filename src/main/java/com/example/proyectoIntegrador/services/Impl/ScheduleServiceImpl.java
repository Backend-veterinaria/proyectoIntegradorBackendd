package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.ScheduleModels.Availability;
import com.example.proyectoIntegrador.models.ScheduleModels.SystemParameter;
import com.example.proyectoIntegrador.models.ScheduleModels.TimeSlotRange;
import com.example.proyectoIntegrador.models.ScheduleModels.WeekSchedule;
import com.example.proyectoIntegrador.models.ScheduleModels.WeekScheduleDTO;
import com.example.proyectoIntegrador.repository.AgendaCitaRepository;
import com.example.proyectoIntegrador.repository.ScheduleRepository;
import com.example.proyectoIntegrador.repository.ScheduleRepository.OwnedRange;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import com.example.proyectoIntegrador.services.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

    static final String DURATION_KEY = "duracion_cita_minutos";
    private static final int MIN_DURATION = 5;
    private static final int MAX_DURATION = 240;

    /** Validación de cada parámetro conocido: clave -> mensaje si el valor no es válido. */
    private static final Map<String, String> KNOWN_PARAMETERS = Map.of(
            DURATION_KEY, "La duracion de la cita debe ser un numero entre " + MIN_DURATION + " y " + MAX_DURATION);

    private static final DateTimeFormatter STRICT_DATE =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private final ScheduleRepository scheduleRepository;
    private final AgendaCitaRepository agendaCitaRepository;
    private final VeterinarianRepository veterinarianRepository;

    @Override
    public int appointmentMinutes() {
        return scheduleRepository.findParameter(DURATION_KEY)
                .flatMap(ScheduleServiceImpl::parseDuration)
                .orElse(AgendaCitaRepository.APPOINTMENT_MINUTES);
    }

    @Override
    public void requireWithinSchedule(int medicoId, LocalDate fecha, LocalTime hora, int minutes) {
        if (!scheduleRepository.hasAnySchedule()) {
            return;
        }
        LocalTime fin = hora.plusMinutes(minutes);
        boolean dentro = rangesOfDay(medicoId, fecha).stream().anyMatch(r ->
                !hora.isBefore(LocalTime.parse(r.getHoraInicio()))
                        && !fin.isAfter(LocalTime.parse(r.getHoraFin()))
                        && fin.isAfter(hora)); // descarta citas que pasarían de medianoche
        if (!dentro) {
            throw new BusinessException("El veterinario no atiende en ese horario");
        }
    }

    @Override
    public Availability availability(int medicoId, String fechaText) {
        LocalDate fecha = parseDate(fechaText);
        agendaCitaRepository.validateVeterinarian(medicoId)
                .orElseThrow(() -> new BusinessException("El veterinario no existe o esta inactivo"));
        if (!scheduleRepository.hasAnySchedule()) {
            throw new BusinessException("No hay horarios de atencion configurados");
        }

        int minutes = appointmentMinutes();
        List<LocalTime> ocupadas = agendaCitaRepository.findBusyTimes(medicoId, fecha);
        LocalDateTime ahora = LocalDateTime.now();
        List<String> libres = new ArrayList<>();

        for (TimeSlotRange r : rangesOfDay(medicoId, fecha)) {
            LocalTime fin = LocalTime.parse(r.getHoraFin());
            LocalTime t = LocalTime.parse(r.getHoraInicio());
            // t + minutes <= fin, sin pasar de medianoche
            while (!t.plusMinutes(minutes).isAfter(fin) && t.plusMinutes(minutes).isAfter(t)) {
                LocalTime slot = t;
                boolean pasada = !LocalDateTime.of(fecha, slot).isAfter(ahora);
                // Mismo criterio que isSlotAvailable: choca con una cita que empiece a menos de \"minutes\"
                boolean ocupada = ocupadas.stream()
                        .anyMatch(o -> Math.abs(java.time.Duration.between(o, slot).toMinutes()) < minutes);
                if (!pasada && !ocupada) {
                    libres.add(slot.toString());
                }
                t = t.plusMinutes(minutes);
            }
        }
        libres.sort(Comparator.naturalOrder());
        return new Availability(fecha.toString(), medicoId, minutes, libres);
    }

    @Override
    public WeekSchedule getWeek(Integer medicoId) {
        if (medicoId == null) {
            return new WeekSchedule(null, true, scheduleRepository.findWeek(null));
        }
        requireVeterinarian(medicoId);
        List<TimeSlotRange> propias = scheduleRepository.findWeek(medicoId);
        return propias.isEmpty()
                ? new WeekSchedule(medicoId, true, scheduleRepository.findWeek(null))
                : new WeekSchedule(medicoId, false, propias);
    }

    @Override
    @Transactional
    public WeekSchedule replaceWeek(Integer medicoId, WeekScheduleDTO dto) {
        if (medicoId != null) {
            requireVeterinarian(medicoId);
        }
        validateWeek(dto.getFranjas());
        scheduleRepository.replaceWeek(medicoId, dto.getFranjas());
        log.info("Horario de {} actualizado: {} franjas", medicoId == null ? "la clinica" : "el medico " + medicoId,
                dto.getFranjas().size());
        return getWeek(medicoId);
    }

    @Override
    public List<SystemParameter> parameters() {
        return scheduleRepository.findParameters();
    }

    @Override
    public SystemParameter updateParameter(String clave, String valor) {
        String limpio = valor.trim();
        if (DURATION_KEY.equals(clave) && parseDuration(limpio).isEmpty()) {
            throw new BusinessException(KNOWN_PARAMETERS.get(DURATION_KEY));
        }
        if (scheduleRepository.updateParameter(clave, limpio) == 0) {
            throw new BusinessException("El parametro no existe");
        }
        return scheduleRepository.findParameters().stream()
                .filter(p -> p.getClave().equals(clave)).findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontro el parametro recien actualizado"));
    }

    /** Franjas del veterinario ese día; si no tiene propias, las de la clínica. */
    private List<TimeSlotRange> rangesOfDay(int medicoId, LocalDate fecha) {
        List<OwnedRange> rows = scheduleRepository.findDay(medicoId, fecha.getDayOfWeek().getValue());
        List<TimeSlotRange> propias = rows.stream().filter(o -> o.medicoId() != null).map(OwnedRange::range).toList();
        return propias.isEmpty()
                ? rows.stream().filter(o -> o.medicoId() == null).map(OwnedRange::range).toList()
                : propias;
    }

    private static void validateWeek(List<TimeSlotRange> franjas) {
        for (TimeSlotRange f : franjas) {
            if (!LocalTime.parse(f.getHoraFin()).isAfter(LocalTime.parse(f.getHoraInicio()))) {
                throw new BusinessException("La hora de fin debe ser posterior a la de inicio (" + f.getHoraInicio()
                        + "-" + f.getHoraFin() + ")");
            }
        }
        for (int i = 0; i < franjas.size(); i++) {
            for (int j = i + 1; j < franjas.size(); j++) {
                TimeSlotRange a = franjas.get(i);
                TimeSlotRange b = franjas.get(j);
                boolean seCruzan = a.getDiaSemana().equals(b.getDiaSemana())
                        && LocalTime.parse(a.getHoraInicio()).isBefore(LocalTime.parse(b.getHoraFin()))
                        && LocalTime.parse(b.getHoraInicio()).isBefore(LocalTime.parse(a.getHoraFin()));
                if (seCruzan) {
                    throw new BusinessException("Hay franjas que se cruzan el dia " + a.getDiaSemana());
                }
            }
        }
    }

    private void requireVeterinarian(int medicoId) {
        veterinarianRepository.findById(medicoId)
                .orElseThrow(() -> new BusinessException("El veterinario no existe"));
    }

    private static java.util.Optional<Integer> parseDuration(String value) {
        try {
            int minutes = Integer.parseInt(value.trim());
            return minutes >= MIN_DURATION && minutes <= MAX_DURATION
                    ? java.util.Optional.of(minutes) : java.util.Optional.empty();
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }

    private static LocalDate parseDate(String fecha) {
        try {
            return LocalDate.parse(fecha, STRICT_DATE);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessException("La fecha no es valida");
        }
    }
}
