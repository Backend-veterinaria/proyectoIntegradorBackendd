package com.example.proyectoIntegrador.repository;

import com.example.proyectoIntegrador.models.NotificationModels.Notification;
import com.example.proyectoIntegrador.models.NotificationModels.Preference;
import com.example.proyectoIntegrador.models.NotificationModels.SaveResult;

import java.util.List;

/**
 * Notificaciones guardadas y preferencias (CIT-09). Requiere sql/CIT-09_notificaciones.sql.
 */
public interface NotificationRepository {

    /**
     * Guarda el aviso si el tipo está activo y el usuario no lo desactivó. Corre en su propia transacción
     * y nunca lanza excepciones: un fallo aquí no debe deshacer la operación que generó el aviso.
     */
    SaveResult saveIfEnabled(int userId, String tipo, String mensaje, String datosJson);

    List<Notification> findByUser(int userId, boolean soloNoLeidas, int limit);

    int countUnread(int userId);

    /** @return filas actualizadas: 0 si no existe o no es del usuario */
    int markRead(int notificacionId, int userId);

    int markAllRead(int userId);

    /** Todos los tipos activos, con la preferencia del usuario (activa si no la ha cambiado). */
    List<Preference> findPreferences(int userId);

    boolean typeExists(String tipo);

    void savePreference(int userId, String tipo, boolean activa);
}
