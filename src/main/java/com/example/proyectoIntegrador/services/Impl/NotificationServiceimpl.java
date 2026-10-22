package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.models.DetailAppointment;
import com.example.proyectoIntegrador.services.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class NotificationServiceimpl implements NotificationService {

    private final ConcurrentMap<String, SseEmitter> clientes =
            new ConcurrentHashMap<>();

    @Override
    public SseEmitter stablishConnection(String client_id) {

        SseEmitter emitter = new SseEmitter(0L);

        clientes.put(client_id, emitter);

        Runnable eliminarConexion = () -> clientes.remove(client_id);

        emitter.onCompletion(eliminarConexion);
        emitter.onTimeout(() -> {
            eliminarConexion.run();
            emitter.complete();
        });
        emitter.onError(error -> eliminarConexion.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("conectado")
                    .data("Conexión SSE establecida"));
        } catch (IOException e) {
            eliminarConexion.run();
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @Override
    public void notifyAppointmentCreation(String client_id, DetailAppointment dataAppointmentDTO) {
        SseEmitter emitter = clientes.get(client_id);

        if (emitter == null) {
            return;
        }

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("cita-creada")
                            .data(dataAppointmentDTO)
            );
        } catch (IOException e) {
            clientes.remove(client_id, emitter);
            emitter.completeWithError(e);
        }
    }


}
