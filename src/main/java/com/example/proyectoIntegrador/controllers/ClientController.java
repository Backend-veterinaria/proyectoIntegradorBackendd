package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.models.ClientUpdateDTO;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import com.example.proyectoIntegrador.models.StatusDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Gestión de propietarios (CIT-03, RF-03).
 */
@RestController
@RequestMapping("/integrador/clients")
public interface ClientController {

    /**
     * Lista de propietarios para el personal (por ejemplo, para elegir a quién agendar una cita).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    ResponseEntity<ResponseGeneric> list(@RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive,
                                         @RequestParam(name = "q", required = false) String q);

    /**
     * Perfil de un propietario: el cliente el suyo, el personal cualquiera.
     */
    @GetMapping("/{clienteId}")
    ResponseEntity<ResponseGeneric> get(@PathVariable("clienteId") int clienteId);

    /**
     * Edita los datos de contacto (CPA-05): el cliente los suyos, el personal los de cualquiera.
     */
    @PutMapping("/{clienteId}")
    ResponseEntity<ResponseGeneric> update(@PathVariable("clienteId") int clienteId, @Valid @RequestBody ClientUpdateDTO dto);

    /**
     * Activa o inactiva un propietario (CPA-06). Solo administradores.
     */
    @PatchMapping("/{clienteId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<ResponseGeneric> changeStatus(@PathVariable("clienteId") int clienteId, @Valid @RequestBody StatusDTO dto);
}
