package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.models.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/integrador/agendaCita")
public interface AgendaController {

    /**
     * Maneja las solicitudes de inicio de sesión de los usuarios validando las credenciales proporcionadas.
     *
     * @param loginDTO el objeto de transferencia de datos que contiene las credenciales de inicio de sesión del usuario
     * @param request  el objeto de solicitud HTTP
     * @return un ResponseEntity que contiene un objeto ResponseLogin con la información del usuario si el inicio de sesión es exitoso,
     * o un mensaje de error si el inicio de sesión falla
     */
    @GetMapping("/list")
    @ResponseBody
    ResponseEntity<ResponseGeneric> getListAgend(@ModelAttribute @Valid DataUserDTO loginDTO, HttpServletRequest request);


    @PostMapping("/scheduleAnAppointment")
    @ResponseBody
    ResponseEntity<ResponseGeneric> scheduleAnAppointment(@Valid @RequestBody DataAppointmentDTO dataAppointmentDTO, HttpServletRequest request);


    @PatchMapping("/cancelledAppointment")
    @ResponseBody
    ResponseEntity<ResponseGeneric> cancelledAppointment(@Valid @RequestBody DataCancelledAppointmentDTO dataAppointmentDTO, HttpServletRequest request);


    /**
     * Agenda de un veterinario en un día (VET-02). Solo personal: el veterinario ve la suya y el
     * administrador la de cualquier veterinario (indicando medicoId).
     */
    @GetMapping("/veterinarian")
    @ResponseBody
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    ResponseEntity<ResponseGeneric> getVeterinarianAgenda(@ModelAttribute @Valid DataVeterinarianAgendaDTO dto);


    @GetMapping("/AnAppointment/details")
    @ResponseBody
    ResponseEntity<ResponseGeneric> getDetailAppointment(@ModelAttribute @Valid DataDetailAppointmentDTO detailAppointmentDTO, HttpServletRequest request);

}
