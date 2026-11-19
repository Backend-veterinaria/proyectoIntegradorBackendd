package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.models.DetailAppointment;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {

    /*
    Establece la conexión con SSE, se crea el objeto listo para recibir mensajes del servidor
    */
    SseEmitter stablishConnection(String client_id);

    /*
    Notifica al usuario establecido en la conexión SSE, busca e identifica si existe alguna conexión emitter, para notificar la creación de la cita
    */
    void notifyAppointmentCreation(String client_id, DetailAppointment dataAppointmentDTO);

    /**
     * Envía un evento con nombre al usuario, si tiene una conexión abierta (CIT-07: "cita-cancelada",
     * "cita-reprogramada"). Si no está conectado no hace nada; nunca lanza excepciones.
     */
    void notify(String clientId, String event, Object data);

    /** Avisos guardados del usuario, del más reciente al más antiguo (CIT-09). */
    com.example.proyectoIntegrador.models.NotificationModels.Inbox inbox(int userId, boolean soloNoLeidas, int limit);

    /** Marca un aviso propio como leído. */
    void markRead(int userId, int notificacionId);

    void markAllRead(int userId);

    /** Tipos de aviso activos con la preferencia del usuario. */
    java.util.List<com.example.proyectoIntegrador.models.NotificationModels.Preference> preferences(int userId);

    java.util.List<com.example.proyectoIntegrador.models.NotificationModels.Preference> updatePreferences(
            int userId, java.util.List<com.example.proyectoIntegrador.models.NotificationModels.PreferenceItem> items);
}
