package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.security.AuthenticatedUser;
import com.example.proyectoIntegrador.models.*;

import java.util.List;

public interface AgendaService {

    List<AgendaCita> getListCitas(int idClient);

    boolean scheduleAnAppointment(DataAppointmentDTO dataAppointmentDTO);

    AgendaCitaDetail getDetailAppointment(DataDetailAppointmentDTO detailAppointmentDTO);

    boolean cancelledAppointment(DataCancelledAppointmentDTO dataCancelledAppointmentDTO);

    /**
     * Agenda de un veterinario en un día (VET-02). Un VETERINARIO solo puede ver la suya (si no envía
     * medicoId se usa el suyo); un ADMIN debe indicar medicoId.
     *
     * @throws org.springframework.security.access.AccessDeniedException si un veterinario pide la agenda de otro
     */
    List<VeterinarianAgendaItem> getVeterinarianAgenda(AuthenticatedUser user, DataVeterinarianAgendaDTO dto);

    /**
     * Cambia el estado de una cita a confirmada, atendida o no asistió (VET-03). Un VETERINARIO solo
     * puede cambiar sus propias citas; un ADMIN, cualquiera.
     *
     * @return la cita con su nuevo estado
     * @throws org.springframework.security.access.AccessDeniedException si la cita no es del veterinario
     */
    VeterinarianAgendaItem changeAppointmentStatus(AuthenticatedUser user, int agendaId, int estadoId);
}
