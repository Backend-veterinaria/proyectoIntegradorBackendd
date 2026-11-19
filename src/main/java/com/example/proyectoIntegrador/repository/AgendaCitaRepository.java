package com.example.proyectoIntegrador.repository;

import com.example.proyectoIntegrador.enums.AppointmentStatus;
import com.example.proyectoIntegrador.models.AgendaCita;
import com.example.proyectoIntegrador.models.AgendaCitaDetail;
import com.example.proyectoIntegrador.models.InfoMascota;
import com.example.proyectoIntegrador.models.VeterinarianAgendaItem;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AgendaCitaRepository {

    Optional<List<AgendaCita>> getListAgenda(int idClient);

    Optional<Boolean> validateClient(int idClient);

    Optional<Boolean> validateVeterinarian(int idVeterinarian);

    Optional<Integer> insertAgendaCita(String description, String fecha, LocalTime hora, Integer estadoId, Integer clienteId, Integer medicoId, Integer mascotaId);

    Optional<AgendaCitaDetail> getDetailAppointment(int idAgenda, int idClient);

    /**
     * Citas asignadas a un veterinario en un día, ordenadas por hora (VET-02).
     *
     * @param estadoId opcional: si no es nulo, solo las citas en ese estado
     * @return la lista de citas (vacía si no tiene citas ese día)
     */
    List<VeterinarianAgendaItem> getAgendaByVeterinarian(int medicoId, LocalDate fecha, Integer estadoId);

    /**
     * Una cita por su agenda_id, con su veterinario, cliente, mascota y estado (VET-03).
     *
     * @return la cita, o vacío si no existe
     */
    Optional<VeterinarianAgendaItem> findAgendaItem(int agendaId);

    /**
     * @return la descripción del estado en estado_cita, o vacío si el estado no existe
     */
    Optional<String> getEstadoDescription(int estadoId);

    /**
     * Cambia el estado de la cita solo si sigue en el estado esperado (evita pisar un cambio
     * hecho por otra persona al mismo tiempo). No captura excepciones.
     *
     * @return el número de filas actualizadas: 0 si la cita ya no estaba en currentEstadoId
     */
    int updateEstado(int agendaId, int currentEstadoId, int newEstadoId);

    /**
     * Cancela la cita y guarda el motivo (CIT-07), solo si sigue en el estado esperado. No captura excepciones.
     *
     * @param motivo opcional, puede ser null
     * @return filas actualizadas: 0 si la cita ya no estaba en currentEstadoId
     */
    int cancel(int agendaId, int currentEstadoId, int canceladaEstadoId, String motivo);

    Optional<InfoMascota> getInfoMascota(int idClient, int mascotaId);

    /**
     * Indica si la mascota existe, pertenece al cliente y está activa. No captura excepciones.
     */
    boolean isMascotaOfClient(int mascotaId, int clienteId);

    /**
     * Indica si el veterinario tiene libre el horario: no tiene otra cita (que no esté cancelada)
     * a menos de {@code minutes} minutos de esa hora (la duración de una cita, CIT-08). No captura excepciones.
     *
     * @param excludeAgendaId cita que no se tiene en cuenta (la que se está reprogramando), o null
     */
    boolean isSlotAvailable(int medicoId, LocalDate fecha, LocalTime hora, Integer excludeAgendaId, int minutes);

    /**
     * Horas de inicio de las citas del veterinario ese día que ocupan horario (todas menos las
     * canceladas), para calcular las horas libres (CIT-08). No captura excepciones.
     */
    List<LocalTime> findBusyTimes(int medicoId, LocalDate fecha);

    /**
     * Busca en estado_cita el id del estado, reconociéndolo por su descripción.
     *
     * @return el estado_id (el menor, si hay varios), o vacío si no existe ese estado
     */
    Optional<Integer> findEstadoId(AppointmentStatus status);

    /**
     * Cambia fecha, hora, veterinario y estado de la cita, solo si sigue en el estado esperado.
     * Debe llamarse dentro de una transacción. No captura excepciones.
     *
     * @return el número de citas actualizadas: 0 si la cita ya no estaba en currentEstadoId
     */
    int reschedule(int agendaId, int currentEstadoId, LocalDate fecha, LocalTime hora, int medicoId, int newEstadoId);

    /** Duración de una cita si no está configurada en parametro_sistema (duracion_cita_minutos). */
    int APPOINTMENT_MINUTES = 30;

}
