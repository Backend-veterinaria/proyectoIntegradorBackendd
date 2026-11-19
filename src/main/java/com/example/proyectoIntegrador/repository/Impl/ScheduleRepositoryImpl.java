package com.example.proyectoIntegrador.repository.Impl;

import com.example.proyectoIntegrador.models.ScheduleModels.SystemParameter;
import com.example.proyectoIntegrador.models.ScheduleModels.TimeSlotRange;
import com.example.proyectoIntegrador.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ScheduleRepositoryImpl implements ScheduleRepository {

    private final JdbcTemplate jdbcTemplate;

    private static TimeSlotRange range(ResultSet rs) throws SQLException {
        return new TimeSlotRange(rs.getInt("dia_semana"),
                rs.getTime("hora_inicio").toLocalTime().toString().substring(0, 5),
                rs.getTime("hora_fin").toLocalTime().toString().substring(0, 5));
    }

    @Override
    public boolean hasAnySchedule() {
        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM horario_atencion WHERE activo)", Boolean.class));
        } catch (Exception e) {
            log.warn("No se pudieron consultar los horarios (¿se ejecutó sql/CIT-08?): {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<OwnedRange> findDay(int medicoId, int diaSemana) {
        String sql = """
                SELECT medico_id, dia_semana, hora_inicio, hora_fin FROM horario_atencion
                WHERE activo AND dia_semana = ? AND (medico_id = ? OR medico_id IS NULL)
                ORDER BY hora_inicio
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new OwnedRange(rs.getObject("medico_id", Integer.class), range(rs)), diaSemana, medicoId);
    }

    @Override
    public List<TimeSlotRange> findWeek(Integer medicoId) {
        String sql = """
                SELECT dia_semana, hora_inicio, hora_fin FROM horario_atencion
                WHERE activo AND medico_id IS NOT DISTINCT FROM ?
                ORDER BY dia_semana, hora_inicio
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> range(rs), medicoId);
    }

    @Override
    public void replaceWeek(Integer medicoId, List<TimeSlotRange> franjas) {
        jdbcTemplate.update("DELETE FROM horario_atencion WHERE medico_id IS NOT DISTINCT FROM ?", medicoId);
        for (TimeSlotRange f : franjas) {
            jdbcTemplate.update("""
                            INSERT INTO horario_atencion (medico_id, dia_semana, hora_inicio, hora_fin)
                            VALUES (?, ?, ?, ?)
                            """, medicoId, f.getDiaSemana(),
                    Time.valueOf(LocalTime.parse(f.getHoraInicio())), Time.valueOf(LocalTime.parse(f.getHoraFin())));
        }
    }

    @Override
    public List<SystemParameter> findParameters() {
        return jdbcTemplate.query("SELECT clave, valor, descripcion FROM parametro_sistema ORDER BY clave",
                (rs, rowNum) -> new SystemParameter(rs.getString("clave"), rs.getString("valor"), rs.getString("descripcion")));
    }

    @Override
    public Optional<String> findParameter(String clave) {
        try {
            return jdbcTemplate.queryForList("SELECT valor FROM parametro_sistema WHERE clave = ?", String.class, clave)
                    .stream().findFirst();
        } catch (Exception e) {
            log.warn("No se pudo leer el parámetro {} (¿se ejecutó sql/CIT-08?): {}", clave, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public int updateParameter(String clave, String valor) {
        return jdbcTemplate.update("UPDATE parametro_sistema SET valor = ? WHERE clave = ?", valor, clave);
    }
}
