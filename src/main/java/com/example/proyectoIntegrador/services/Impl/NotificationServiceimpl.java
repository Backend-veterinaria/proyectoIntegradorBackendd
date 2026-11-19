package com.example.proyectoIntegrador.services.Impl;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.AppointmentEvent;
import com.example.proyectoIntegrador.models.DetailAppointment;
import com.example.proyectoIntegrador.models.NotificationModels.Inbox;
import com.example.proyectoIntegrador.models.NotificationModels.Preference;
import com.example.proyectoIntegrador.models.NotificationModels.PreferenceItem;
import com.example.proyectoIntegrador.models.NotificationModels.SaveResult;
import com.example.proyectoIntegrador.repository.NotificationRepository;
import com.example.proyectoIntegrador.services.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceimpl implements NotificationService {

    /** Máximo de avisos que devuelve la bandeja en una consulta. */
    static final int MAX_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

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
        notify(client_id, "cita-creada", dataAppointmentDTO);
    }

    @Override
    public void notify(String clientId, String event, Object data) {
        // Primero se guarda (CIT-09). Si el usuario desactivó este tipo de aviso, tampoco se envía.
        if (save(clientId, event, data) == SaveResult.DISABLED) {
            return;
        }
        send(clientId, event, data);
    }

    private SaveResult save(String clientId, String event, Object data) {
        try {
            int userId = Integer.parseInt(clientId);
            return notificationRepository.saveIfEnabled(userId, event, message(event, data),
                    objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            log.warn("No se pudo guardar el aviso {} del usuario {}: {}", event, clientId, e.getMessage());
            return SaveResult.UNAVAILABLE;
        }
    }

    /** Texto legible del aviso, para la bandeja de notificaciones. */
    static String message(String event, Object data) {
        String detalle = "";
        if (data instanceof DetailAppointment d) {
            detalle = describe(d.description(), d.fecha(), d.hora());
        } else if (data instanceof AppointmentEvent e) {
            detalle = describe(e.descripcion(), e.fecha(), e.hora())
                    + (e.motivo() != null ? ". Motivo: " + e.motivo() : "");
        }
        String accion = switch (event) {
            case "cita-creada" -> "Se agendo tu cita";
            case "cita-cancelada" -> "Se cancelo la cita";
            case "cita-reprogramada" -> "Se reprogramo la cita";
            default -> "Hay una novedad en tus citas";
        };
        String texto = accion + detalle;
        return texto.length() > 500 ? texto.substring(0, 500) : texto;
    }

    /**
     * Por ejemplo: ' "Vacunacion" del 2026-11-20 a las 14:30'. Acepta fechas yyyyMMdd o yyyy-MM-dd y horas
     * HHmmss o HH:mm:ss (el evento de creación y los de cambio usan formatos distintos).
     */
    private static String describe(String descripcion, String fecha, String hora) {
        StringBuilder sb = new StringBuilder();
        if (descripcion != null && !descripcion.isBlank()) {
            sb.append(" \"").append(descripcion.trim()).append('"');
        }
        if (fecha != null) {
            String f = fecha.matches("[0-9]{8}") ? fecha.substring(0, 4) + "-" + fecha.substring(4, 6) + "-" + fecha.substring(6) : fecha;
            sb.append(" del ").append(f);
        }
        if (hora != null) {
            String h = hora.matches("[0-9]{6}") ? hora.substring(0, 2) + ":" + hora.substring(2, 4)
                    : hora.length() >= 5 ? hora.substring(0, 5) : hora;
            sb.append(" a las ").append(h);
        }
        return sb.toString();
    }

    @Override
    public Inbox inbox(int userId, boolean soloNoLeidas, int limit) {
        int max = Math.max(1, Math.min(limit, MAX_LIMIT));
        return new Inbox(notificationRepository.countUnread(userId),
                notificationRepository.findByUser(userId, soloNoLeidas, max));
    }

    @Override
    public void markRead(int userId, int notificacionId) {
        if (notificationRepository.markRead(notificacionId, userId) == 0) {
            throw new BusinessException("La notificacion no existe");
        }
    }

    @Override
    public void markAllRead(int userId) {
        notificationRepository.markAllRead(userId);
    }

    @Override
    public List<Preference> preferences(int userId) {
        return notificationRepository.findPreferences(userId);
    }

    @Override
    public List<Preference> updatePreferences(int userId, List<PreferenceItem> items) {
        for (PreferenceItem item : items) {
            if (!notificationRepository.typeExists(item.getTipo())) {
                throw new BusinessException("El tipo de notificacion " + item.getTipo() + " no existe");
            }
        }
        items.forEach(item -> notificationRepository.savePreference(userId, item.getTipo(), item.getActiva()));
        return notificationRepository.findPreferences(userId);
    }

    private void send(String clientId, String event, Object data) {
        SseEmitter emitter = clientes.get(clientId);

        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            clientes.remove(clientId, emitter);
            emitter.completeWithError(e);
        } catch (IllegalStateException e) {
            // La conexión ya se había cerrado (el cliente se desconectó y aún no se quitó del mapa).
            // La notificación es opcional: no debe hacer fallar la operación que la originó.
            clientes.remove(clientId, emitter);
        }
    }
}
