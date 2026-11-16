package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.models.ClinicalAttentionCreateDTO;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Historia clínica de las mascotas (RF-09). Requiere sql/HC-01_atencion_clinica.sql.
 */
@RestController
@RequestMapping("/integrador/clinicalHistory")
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
}
