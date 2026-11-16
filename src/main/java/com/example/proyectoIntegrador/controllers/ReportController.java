package com.example.proyectoIntegrador.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.proyectoIntegrador.models.ReportFilterDTO;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reportes (RF-12). Solo personal: el administrador ve todo (o filtra por medicoId) y el
 * veterinario solo sus propios datos. Parámetros: desde y hasta (yyyyMMdd, inclusivos), medicoId opcional.
 */
@RestController
@RequestMapping("/integrador/reports")
@PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
@Tag(name = "Reportes", description = "Reportes de citas, atenciones y mascotas atendidas (personal)")
public interface ReportController {

    /** REP-01: citas por estado y período. */
    @GetMapping("/appointmentsByStatus")
    ResponseEntity<ResponseGeneric> appointmentsByStatus(@ModelAttribute @Valid ReportFilterDTO filter);

    /** REP-02: atenciones por veterinario. */
    @GetMapping("/attentionsByVeterinarian")
    ResponseEntity<ResponseGeneric> attentionsByVeterinarian(@ModelAttribute @Valid ReportFilterDTO filter);

    /** REP-03: mascotas atendidas. */
    @GetMapping("/attendedPets")
    ResponseEntity<ResponseGeneric> attendedPets(@ModelAttribute @Valid ReportFilterDTO filter);
}
