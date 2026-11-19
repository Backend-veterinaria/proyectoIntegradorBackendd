package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.models.ResponseGeneric;
import com.example.proyectoIntegrador.models.ScheduleModels.ParameterValueDTO;
import com.example.proyectoIntegrador.models.ScheduleModels.WeekScheduleDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Horarios de atención y parámetros del sistema (CIT-08). Las horas libres para agendar se consultan
 * en GET /integrador/agendaCita/availability.
 */
@RestController
@RequestMapping("/integrador")
@Tag(name = "Horarios y parámetros", description = "Horario de atención de la clínica y de cada veterinario, y parámetros del sistema")
public interface ScheduleController {

    /**
     * Horario semanal: sin medicoId, el de la clínica; con medicoId, el del veterinario (o el de la
     * clínica si no tiene uno propio, con usaHorarioGeneral=true). Solo personal.
     */
    @GetMapping("/schedules")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    ResponseEntity<ResponseGeneric> getWeek(@RequestParam(name = "medicoId", required = false) Integer medicoId);

    /**
     * Reemplaza el horario semanal completo (de la clínica o del veterinario). Una lista vacía de franjas
     * en un veterinario hace que vuelva a usar el horario de la clínica. Solo administradores.
     */
    @PutMapping("/schedules")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseGeneric> replaceWeek(@RequestParam(name = "medicoId", required = false) Integer medicoId,
                                                @Valid @RequestBody WeekScheduleDTO dto);

    @GetMapping("/parameters")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseGeneric> parameters();

    @PutMapping("/parameters/{clave}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseGeneric> updateParameter(@PathVariable("clave") String clave,
                                                    @Valid @RequestBody ParameterValueDTO dto);
}
