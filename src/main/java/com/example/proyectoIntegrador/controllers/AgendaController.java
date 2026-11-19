package com.example.proyectoIntegrador.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.proyectoIntegrador.models.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/integrador/agendaCita")
@Tag(name = "Citas", description = "Agendar, consultar, cancelar, reprogramar y cambiar el estado de citas; agenda del veterinario")
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


    /**
     * Cambia el estado de una cita a confirmada, atendida o no asistió (VET-03). Solo personal:
     * el veterinario sus propias citas y el administrador cualquiera.
     */
    @PatchMapping("/{agendaId}/status")
    @ResponseBody
    @PreAuthorize("hasAnyRole('VETERINARIO', 'ADMIN')")
    ResponseEntity<ResponseGeneric> changeAppointmentStatus(@PathVariable("agendaId") int agendaId,
                                                            @Valid @RequestBody AppointmentStatusDTO dto);


    /**
     * Reprograma una cita a otra fecha y hora (VET-04). Cualquier usuario autenticado; el servicio
     * valida que el cliente sea el dueño de la cita o que el veterinario sea el asignado.
     */
    @PatchMapping("/{agendaId}/reschedule")
    @ResponseBody
    ResponseEntity<ResponseGeneric> rescheduleAppointment(@PathVariable("agendaId") int agendaId,
                                                          @Valid @RequestBody RescheduleAppointmentDTO dto);


    /**
     * Horas libres de un veterinario en un día, según su horario de atención (CIT-08). Cualquier usuario
     * autenticado. fecha en formato yyyyMMdd; las horas se devuelven como HH:mm.
     */
    @GetMapping("/availability")
    ResponseEntity<ResponseGeneric> getAvailability(@RequestParam("medicoId") int medicoId,
                                                    @RequestParam("fecha") String fecha);


    @GetMapping("/AnAppointment/details")
    @ResponseBody
    ResponseEntity<ResponseGeneric> getDetailAppointment(@ModelAttribute @Valid DataDetailAppointmentDTO detailAppointmentDTO, HttpServletRequest request);

}
