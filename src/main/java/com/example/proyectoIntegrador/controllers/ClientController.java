package com.example.proyectoIntegrador.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.proyectoIntegrador.models.ClientCreateDTO;
import com.example.proyectoIntegrador.models.ClientUpdateDTO;
import com.example.proyectoIntegrador.models.PasswordChangeDTO;
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
@Tag(name = "Propietarios", description = "Registro por el personal, perfil, búsqueda, activación y contraseñas de propietarios")
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

    /**
     * El personal registra un propietario desde la clínica (CIT-06). Responde con una contraseña temporal
     * que se muestra una sola vez; el tutor debe cambiarla en su primer inicio de sesión.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    ResponseEntity<ResponseGeneric> create(@Valid @RequestBody ClientCreateDTO dto);

    /**
     * El personal asigna una nueva contraseña temporal a un propietario que olvidó la suya (CIT-06).
     */
    @PostMapping("/{clienteId}/password/reset")
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    ResponseEntity<ResponseGeneric> resetPassword(@PathVariable("clienteId") int clienteId);

    /**
     * El usuario cambia su propia contraseña dando la actual (CIT-06). Sirve para cualquier rol.
     */
    @PutMapping("/{clienteId}/password")
    ResponseEntity<ResponseGeneric> changePassword(@PathVariable("clienteId") int clienteId,
                                                   @Valid @RequestBody PasswordChangeDTO dto);
}
