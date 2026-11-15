package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.models.ResponseGeneric;
import com.example.proyectoIntegrador.models.VeterinarianCreateDTO;
import com.example.proyectoIntegrador.models.VeterinarianStatusDTO;
import com.example.proyectoIntegrador.models.VeterinarianUpdateDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Gestión de veterinarios (VET-01, RF-05). Solo para administradores.
 * Para elegir veterinario al agendar una cita se usa el selector de /integrador/selectors/All,
 * que solo incluye veterinarios activos.
 */
// @RestController también en la interfaz (como los demás controladores): @PreAuthorize envuelve el
// bean en un proxy y así Spring MVC lo sigue reconociendo como controlador con cualquier tipo de proxy.
@RestController
@RequestMapping("/integrador/veterinarians")
@PreAuthorize("hasRole('ADMIN')")
public interface VeterinarianController {

    /**
     * Crea el veterinario y su usuario para iniciar sesión (rol VETERINARIO).
     */
    @PostMapping
    ResponseEntity<ResponseGeneric> create(@Valid @RequestBody VeterinarianCreateDTO dto);

    /**
     * Lista los veterinarios. Por defecto solo los activos; con includeInactive=true, todos.
     */
    @GetMapping
    ResponseEntity<ResponseGeneric> list(@RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive);

    @GetMapping("/{medicoId}")
    ResponseEntity<ResponseGeneric> get(@PathVariable("medicoId") int medicoId);

    @PutMapping("/{medicoId}")
    ResponseEntity<ResponseGeneric> update(@PathVariable("medicoId") int medicoId, @Valid @RequestBody VeterinarianUpdateDTO dto);

    /**
     * Activa o inactiva el veterinario ({"activo": false} para inactivar).
     */
    @PatchMapping("/{medicoId}/status")
    ResponseEntity<ResponseGeneric> changeStatus(@PathVariable("medicoId") int medicoId, @Valid @RequestBody VeterinarianStatusDTO dto);
}
