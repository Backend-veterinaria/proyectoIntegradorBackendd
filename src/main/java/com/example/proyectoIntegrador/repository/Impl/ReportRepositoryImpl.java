package com.example.proyectoIntegrador.repository.Impl;

import com.example.proyectoIntegrador.enums.AppointmentStatus;
import com.example.proyectoIntegrador.models.ReportRows;
import com.example.proyectoIntegrador.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ReportRepositoryImpl implements ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<ReportRows.AppointmentsByStatus> appointmentsByStatus(LocalDate desde, LocalDate hasta, Integer medicoId) {
        log.debug("start appointmentsByStatus {} - {} medico={}", desde, hasta, medicoId);

        // Se parte de estado_cita para que aparezcan también los estados sin citas.
        // CAST: PostgreSQL necesita conocer el tipo del parámetro cuando se envía nulo.
        String sql = """
                SELECT ec.estado_id, ec.descripcion, COUNT(c.cita_id) AS cantidad
                FROM estado_cita ec
                LEFT JOIN agenda_cita ac ON ac.estado_id = ec.estado_id
                                        AND (CAST(? AS INTEGER) IS NULL OR ac.medico_id = ?)
                LEFT JOIN cita c ON c.cita_id = ac.cita_id AND c.fecha BETWEEN ? AND ?
                GROUP BY ec.estado_id, ec.descripcion
                ORDER BY ec.estado_id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ReportRows.AppointmentsByStatus(
                        rs.getInt("estado_id"),
                        rs.getString("descripcion"),
                        AppointmentStatus.fromDescription(rs.getString("descripcion")).name(),
                        rs.getLong("cantidad")),
                medicoId, medicoId, Date.valueOf(desde), Date.valueOf(hasta));
    }

    @Override
    public List<ReportRows.AttentionsByVeterinarian> attentionsByVeterinarian(LocalDate desde, LocalDate hasta,
                                                                              Integer medicoId) {
        log.debug("start attentionsByVeterinarian {} - {} medico={}", desde, hasta, medicoId);

        // Todos los veterinarios activos (aunque tengan 0) y los inactivos que atendieron en el período
        String sql = """
                SELECT me.medico_id, p.nombre, p.apellido, COALESCE(p.activo, TRUE) AS activo,
                       COUNT(a.atencion_id) AS atenciones, COUNT(DISTINCT a.mascota_id) AS mascotas
                FROM medico me
                JOIN personal p ON p.personal_id = me.identificacion_medico
                LEFT JOIN atencion_clinica a ON a.medico_id = me.medico_id
                                            AND a.fecha_atencion >= ? AND a.fecha_atencion < ?
                WHERE (CAST(? AS INTEGER) IS NULL OR me.medico_id = ?)
                GROUP BY me.medico_id, p.nombre, p.apellido, p.activo
                HAVING COALESCE(p.activo, TRUE) OR COUNT(a.atencion_id) > 0
                ORDER BY atenciones DESC, p.apellido, p.nombre
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ReportRows.AttentionsByVeterinarian(
                        rs.getInt("medico_id"),
                        joinName(rs.getString("nombre"), rs.getString("apellido")),
                        rs.getBoolean("activo"),
                        rs.getLong("atenciones"),
                        rs.getLong("mascotas")),
                startOf(desde), startOf(hasta.plusDays(1)), medicoId, medicoId);
    }

    @Override
    public List<ReportRows.AttendedPet> attendedPets(LocalDate desde, LocalDate hasta, Integer medicoId) {
        log.debug("start attendedPets {} - {} medico={}", desde, hasta, medicoId);

        String sql = """
                SELECT m.mascota_id, m.nombre AS mascota_nombre, r.descripcion AS raza, m.cliente_id,
                       cl.nombre AS cliente_nombre, cl.apellido AS cliente_apellido,
                       COUNT(*) AS atenciones, MAX(a.fecha_atencion) AS ultima_atencion
                FROM atencion_clinica a
                JOIN mascota m ON m.mascota_id = a.mascota_id
                LEFT JOIN raza r ON r.raza_id = m.raza_id
                LEFT JOIN cliente cl ON cl.cliente_id = m.cliente_id
                WHERE a.fecha_atencion >= ? AND a.fecha_atencion < ?
                  AND (CAST(? AS INTEGER) IS NULL OR a.medico_id = ?)
                GROUP BY m.mascota_id, m.nombre, r.descripcion, m.cliente_id, cl.nombre, cl.apellido
                ORDER BY ultima_atencion DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
                    Timestamp ultima = rs.getTimestamp("ultima_atencion");
                    return new ReportRows.AttendedPet(
                            rs.getInt("mascota_id"),
                            rs.getString("mascota_nombre"),
                            rs.getString("raza"),
                            rs.getObject("cliente_id", Integer.class),
                            joinName(rs.getString("cliente_nombre"), rs.getString("cliente_apellido")),
                            rs.getLong("atenciones"),
                            ultima == null ? null : ultima.toLocalDateTime().withNano(0).toString());
                },
                startOf(desde), startOf(hasta.plusDays(1)), medicoId, medicoId);
    }

    /** Inicio del día: fecha_atencion es TIMESTAMP, así el día "hasta" queda completo. */
    private static Timestamp startOf(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }

    private static String joinName(String nombre, String apellido) {
        if (nombre == null) {
            return apellido;
        }
        return apellido == null ? nombre : nombre + " " + apellido;
    }
}
