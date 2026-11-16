package com.example.proyectoIntegrador.repository.Impl;

import com.example.proyectoIntegrador.models.ClinicalAttention;
import com.example.proyectoIntegrador.models.ClinicalAttentionCreateDTO;
import com.example.proyectoIntegrador.repository.ClinicalHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ClinicalHistoryRepositoryImpl implements ClinicalHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SELECT_ATTENTION = """
            SELECT a.atencion_id, a.agenda_id, a.mascota_id, m.nombre AS mascota_nombre, a.medico_id,
                   p.nombre AS medico_nombre, p.apellido AS medico_apellido, a.fecha_atencion,
                   a.motivo_consulta, a.diagnostico, a.procedimientos, a.observaciones, a.peso_kg, a.temperatura_c
            FROM atencion_clinica a
            LEFT JOIN mascota m ON m.mascota_id = a.mascota_id
            LEFT JOIN medico me ON me.medico_id = a.medico_id
            LEFT JOIN personal p ON p.personal_id = me.identificacion_medico
            """;

    private static final RowMapper<ClinicalAttention> ATTENTION_MAPPER = (rs, rowNum) -> {
        ClinicalAttention a = new ClinicalAttention();
        a.setAtencionId(rs.getInt("atencion_id"));
        a.setAgendaId(rs.getInt("agenda_id"));
        a.setMascotaId(rs.getInt("mascota_id"));
        a.setMascotaNombre(rs.getString("mascota_nombre"));
        a.setMedicoId(rs.getInt("medico_id"));
        String nombre = rs.getString("medico_nombre");
        String apellido = rs.getString("medico_apellido");
        a.setMedicoNombre(nombre == null ? apellido : apellido == null ? nombre : nombre + " " + apellido);
        Timestamp fecha = rs.getTimestamp("fecha_atencion");
        a.setFechaAtencion(fecha == null ? null : fecha.toLocalDateTime().withNano(0).toString());
        a.setMotivoConsulta(rs.getString("motivo_consulta"));
        a.setDiagnostico(rs.getString("diagnostico"));
        a.setProcedimientos(rs.getString("procedimientos"));
        a.setObservaciones(rs.getString("observaciones"));
        a.setPesoKg(rs.getBigDecimal("peso_kg"));
        a.setTemperaturaC(rs.getBigDecimal("temperatura_c"));
        return a;
    };

    @Override
    public boolean existsByAgendaId(int agendaId) {
        String sql = """
                SELECT EXISTS (SELECT 1 FROM atencion_clinica WHERE agenda_id = ?)
                """;

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, agendaId));
    }

    @Override
    public int insert(ClinicalAttentionCreateDTO dto, int mascotaId, int medicoId) {
        log.debug("start insert atencion_clinica agenda={}", dto.getAgendaId());

        String sql = """
                INSERT INTO atencion_clinica (agenda_id, mascota_id, medico_id, motivo_consulta, diagnostico,
                                              procedimientos, observaciones, peso_kg, temperatura_c)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING atencion_id
                """;

        Integer id = jdbcTemplate.queryForObject(sql, Integer.class, dto.getAgendaId(), mascotaId, medicoId,
                dto.getMotivoConsulta(), dto.getDiagnostico(), dto.getProcedimientos(), dto.getObservaciones(),
                dto.getPesoKg(), dto.getTemperaturaC());
        if (id == null) {
            throw new IllegalStateException("La base de datos no devolvio el atencion_id");
        }
        return id;
    }

    @Override
    public Optional<ClinicalAttention> findById(int atencionId) {
        try {
            return jdbcTemplate.query(SELECT_ATTENTION + " WHERE a.atencion_id = ?", ATTENTION_MAPPER, atencionId)
                    .stream().findFirst();
        } catch (Exception e) {
            log.error("No se pudo consultar la atencion {}: {}", atencionId, e.getMessage());
            return Optional.empty();
        }
    }
}
