package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.services.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class NotificationServiceimpl implements NotificationService {
    @Override
    public void notifySSE(String client_id) {

    }
        private final ConcurrentMap<String, SseEmitter> clientes =
                new ConcurrentHashMap<>();

        public SseEmitter conectar() {
            String clienteId = UUID.randomUUID().toString();

            // Conexión de larga duración
            SseEmitter emitter = new SseEmitter(0L);

            clientes.put(clienteId, emitter);

            Runnable eliminarConexion = () -> clientes.remove(clienteId);

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
}
