package com.example.proyectoIntegrador.models;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Notificaciones guardadas y preferencias (CIT-09).
 */
public final class NotificationModels {

    private NotificationModels() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Notification {
        private Integer notificacionId;
        /** Código del tipo, igual al nombre del evento SSE (cita-creada, cita-cancelada...). */
        private String tipo;
        private String titulo;
        private String mensaje;
        /** Los mismos datos que llevó el evento SSE. */
        private JsonNode datos;
        private boolean leida;
        /** ISO, yyyy-MM-ddTHH:mm:ss. */
        private String fecha;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Inbox {
        private int noLeidas;
        private List<Notification> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Preference {
        private String tipo;
        private String titulo;
        private String descripcion;
        private boolean activa;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreferenceItem {
        @NotBlank(message = "required field tipo")
        private String tipo;
        @NotNull(message = "required field activa")
        private Boolean activa;
    }

    @Data
    public static class PreferencesDTO {
        @NotNull(message = "required field preferencias")
        private List<@Valid PreferenceItem> preferencias;
    }

    /** Resultado de intentar guardar un aviso. */
    public enum SaveResult {
        /** Guardado: se envía también en tiempo real. */
        SAVED,
        /** El usuario lo desactivó, o el tipo está desactivado: no se guarda ni se envía. */
        DISABLED,
        /** No se pudo guardar (por ejemplo, falta el script CIT-09): se envía igual en tiempo real. */
        UNAVAILABLE
    }
}
