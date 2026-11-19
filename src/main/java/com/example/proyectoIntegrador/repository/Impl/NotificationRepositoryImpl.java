package com.example.proyectoIntegrador.repository.Impl;

import com.example.proyectoIntegrador.models.NotificationModels.Notification;
import com.example.proyectoIntegrador.models.NotificationModels.Preference;
import com.example.proyectoIntegrador.models.NotificationModels.SaveResult;
import com.example.proyectoIntegrador.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class NotificationRepositoryImpl implements NotificationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    // Transacción propia: si algo falla aquí (por ejemplo, falta la tabla) PostgreSQL aborta solo esta
    // transacción y no la del agendamiento o la reprogramación que generó el aviso.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SaveResult saveIfEnabled(int userId, String tipo, String mensaje, String datosJson) {
        try {
            List<Object[]> tipos = jdbcTemplate.query(
                    "SELECT titulo, activo FROM tipo_notificacion WHERE codigo = ?",
                    (rs, n) -> new Object[]{rs.getString("titulo"), rs.getBoolean("activo")}, tipo);
            if (tipos.isEmpty()) {
                log.warn("El tipo de notificacion {} no esta en tipo_notificacion: no se guarda", tipo);
                return SaveResult.UNAVAILABLE;
            }
            if (!(Boolean) tipos.get(0)[1]) {
                return SaveResult.DISABLED;
            }
            Boolean preferencia = jdbcTemplate.queryForList(
                    "SELECT activa FROM preferencia_notificacion WHERE cliente_id = ? AND tipo = ?",
                    Boolean.class, userId, tipo).stream().findFirst().orElse(true);
            if (!preferencia) {
                return SaveResult.DISABLED;
            }
            jdbcTemplate.update("""
                    INSERT INTO notificacion (cliente_id, tipo, titulo, mensaje, datos) VALUES (?, ?, ?, ?, ?)
                    """, userId, tipo, tipos.get(0)[0], mensaje, datosJson);
            return SaveResult.SAVED;
        } catch (Exception e) {
            log.warn("No se pudo guardar la notificacion {} del usuario {} (¿se ejecutó sql/CIT-09?): {}",
                    tipo, userId, e.getMessage());
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            return SaveResult.UNAVAILABLE;
        }
    }

    @Override
    public List<Notification> findByUser(int userId, boolean soloNoLeidas, int limit) {
        String sql = """
                SELECT notificacion_id, tipo, titulo, mensaje, datos, leida, fecha_creacion
                FROM notificacion
                WHERE cliente_id = ? AND (? = FALSE OR leida = FALSE)
                ORDER BY fecha_creacion DESC, notificacion_id DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (rs, n) -> {
            Timestamp fecha = rs.getTimestamp("fecha_creacion");
            return new Notification(rs.getInt("notificacion_id"), rs.getString("tipo"), rs.getString("titulo"),
                    rs.getString("mensaje"), json(rs.getString("datos")), rs.getBoolean("leida"),
                    fecha == null ? null : fecha.toLocalDateTime().withNano(0).toString());
        }, userId, soloNoLeidas, limit);
    }

    @Override
    public int countUnread(int userId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notificacion WHERE cliente_id = ? AND leida = FALSE", Integer.class, userId);
        return n == null ? 0 : n;
    }

    @Override
    public int markRead(int notificacionId, int userId) {
        return jdbcTemplate.update("UPDATE notificacion SET leida = TRUE WHERE notificacion_id = ? AND cliente_id = ?",
                notificacionId, userId);
    }

    @Override
    public int markAllRead(int userId) {
        return jdbcTemplate.update("UPDATE notificacion SET leida = TRUE WHERE cliente_id = ? AND leida = FALSE", userId);
    }

    @Override
    public List<Preference> findPreferences(int userId) {
        String sql = """
                SELECT t.codigo, t.titulo, t.descripcion, COALESCE(p.activa, TRUE) AS activa
                FROM tipo_notificacion t
                LEFT JOIN preferencia_notificacion p ON p.tipo = t.codigo AND p.cliente_id = ?
                WHERE t.activo
                ORDER BY t.codigo
                """;

        return jdbcTemplate.query(sql, (rs, n) -> new Preference(rs.getString("codigo"), rs.getString("titulo"),
                rs.getString("descripcion"), rs.getBoolean("activa")), userId);
    }

    @Override
    public boolean typeExists(String tipo) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM tipo_notificacion WHERE codigo = ? AND activo)", Boolean.class, tipo));
    }

    @Override
    public void savePreference(int userId, String tipo, boolean activa) {
        jdbcTemplate.update("""
                INSERT INTO preferencia_notificacion (cliente_id, tipo, activa) VALUES (?, ?, ?)
                ON CONFLICT (cliente_id, tipo) DO UPDATE SET activa = EXCLUDED.activa
                """, userId, tipo, activa);
    }

    private JsonNode json(String text) {
        if (text == null) {
            return null;
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            return null;
        }
    }
}
