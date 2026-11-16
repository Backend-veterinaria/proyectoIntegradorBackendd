package com.example.proyectoIntegrador.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.proyectoIntegrador.models.ClinicalAttentionCreateDTO;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import com.example.proyectoIntegrador.models.TreatmentCreateDTO;
import com.example.proyectoIntegrador.models.TreatmentStatusDTO;
import com.example.proyectoIntegrador.models.VaccineCreateDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Historia clínica de las mascotas (RF-09, RF-10, RF-11). Requiere sql/HC-01_atencion_clinica.sql
 * y sql/HC-04_tratamientos_vacunas.sql.
 */
@RestController
@RequestMapping("/integrador/clinicalHistory")
@Tag(name = "Historia clínica", description = "Atención clínica, historial, tratamientos y vacunas")
public interface ClinicalHistoryController {

    /**
     * Registra la atención clínica de una cita y la marca como atendida (HC-01).
     * Solo el veterinario asignado a la cita.
     */
    @PostMapping("/attentions")
    @PreAuthorize("hasRole('VETERINARIO')")
    ResponseEntity<ResponseGeneric> registerAttention(@Valid @RequestBody ClinicalAttentionCreateDTO dto);

    /**
     * Consulta una atención clínica: el personal cualquiera, el dueño las de sus mascotas.
     */
    @GetMapping("/attentions/{atencionId}")
    ResponseEntity<ResponseGeneric> getAttention(@PathVariable("atencionId") int atencionId);

    /**
     * Historia clínica de una mascota (HC-03): sus datos y sus atenciones, de la más reciente a la
     * más antigua. El personal la de cualquier mascota, el dueño la de las suyas (solo lectura).
     */
    @GetMapping("/pets/{mascotaId}")
    ResponseEntity<ResponseGeneric> getPetHistory(@PathVariable("mascotaId") int mascotaId);

    /** Registra un tratamiento (HC-04). Solo veterinarios. */
    @PostMapping("/treatments")
    @PreAuthorize("hasRole('VETERINARIO')")
    ResponseEntity<ResponseGeneric> registerTreatment(@Valid @RequestBody TreatmentCreateDTO dto);

    /** Finaliza o suspende un tratamiento (HC-04). Solo veterinarios. */
    @PatchMapping("/treatments/{tratamientoId}/status")
    @PreAuthorize("hasRole('VETERINARIO')")
    ResponseEntity<ResponseGeneric> changeTreatmentStatus(@PathVariable("tratamientoId") int tratamientoId,
                                                          @Valid @RequestBody TreatmentStatusDTO dto);

    /** Registra una vacuna aplicada (HC-05). Solo veterinarios. */
    @PostMapping("/vaccines")
    @PreAuthorize("hasRole('VETERINARIO')")
    ResponseEntity<ResponseGeneric> registerVaccine(@Valid @RequestBody VaccineCreateDTO dto);

    /**
     * Vacunas con la próxima dosis vencida o que vence en los próximos {@code days} días (HC-05).
     * El personal ve todas (o las de clienteId); el cliente, las de sus mascotas.
     */
    @GetMapping("/vaccines/upcoming")
    ResponseEntity<ResponseGeneric> getUpcomingVaccines(@RequestParam(name = "days", defaultValue = "30") int days,
                                                        @RequestParam(name = "clienteId", required = false) Integer clienteId);
}
