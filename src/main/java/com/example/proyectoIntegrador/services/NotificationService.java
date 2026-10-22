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
}
