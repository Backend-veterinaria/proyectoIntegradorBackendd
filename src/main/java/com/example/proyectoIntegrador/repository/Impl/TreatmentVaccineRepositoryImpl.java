package com.example.proyectoIntegrador.repository.Impl;

import com.example.proyectoIntegrador.enums.TreatmentStatus;
import com.example.proyectoIntegrador.models.Treatment;
import com.example.proyectoIntegrador.models.Vaccine;
import com.example.proyectoIntegrador.repository.TreatmentVaccineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TreatmentVaccineRepositoryImpl implements TreatmentVaccineRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String JOIN_MASCOTA_MEDICO = """
            LEFT JOIN mascota m ON m.mascota_id = x.mascota_id
            LEFT JOIN medico me ON me.medico_id = x.medico_id
            LEFT JOIN personal p ON p.personal_id = me.identificacion_medico
            """;

    private static final String SELECT_TREATMENT = """
            SELECT x.tratamiento_id, x.mascota_id, m.nombre AS mascota_nombre, m.cliente_id, x.atencion_id, x.medico_id,
                   p.nombre AS medico_nombre, p.apellido AS medico_apellido, x.medicamento, x.dosis, x.frecuencia,
                   x.indicaciones, x.fecha_inicio, x.fecha_fin, x.estado
            FROM tratamiento x
            """ + JOIN_MASCOTA_MEDICO;

    private static final String SELECT_VACCINE = """
            SELECT x.vacuna_id, x.mascota_id, m.nombre AS mascota_nombre, m.cliente_id, x.atencion_id, x.medico_id,
                   p.nombre AS medico_nombre, p.apellido AS medico_apellido, x.nombre, x.lote, x.fecha_aplicacion,
                   x.proxima_dosis, x.observaciones
            FROM vacuna x
            """ + JOIN_MASCOTA_MEDICO;

    private static final RowMapper<Treatment> TREATMENT_MAPPER = (rs, rowNum) -> {
        Treatment t = new Treatment();
        t.setTratamientoId(rs.getInt("tratamiento_id"));
        t.setMascotaId(rs.getInt("mascota_id"));
        t.setMascotaNombre(rs.getString("mascota_nombre"));
        t.setClienteId(rs.getObject("cliente_id", Integer.class));
        t.setAtencionId(rs.getObject("atencion_id", Integer.class));
        t.setMedicoId(rs.getInt("medico_id"));
        t.setMedicoNombre(joinName(rs.getString("medico_nombre"), rs.getString("medico_apellido")));
        t.setMedicamento(rs.getString("medicamento"));
        t.setDosis(rs.getString("dosis"));
        t.setFrecuencia(rs.getString("frecuencia"));
        t.setIndicaciones(rs.getString("indicaciones"));
        t.setFechaInicio(isoDate(rs.getDate("fecha_inicio")));
        t.setFechaFin(isoDate(rs.getDate("fecha_fin")));
        t.setEstado(rs.getString("estado"));
        return t;
    };

    private static final RowMapper<Vaccine> VACCINE_MAPPER = (rs, rowNum) -> {
        Vaccine v = new Vaccine();
        v.setVacunaId(rs.getInt("vacuna_id"));
        v.setMascotaId(rs.getInt("mascota_id"));
        v.setMascotaNombre(rs.getString("mascota_nombre"));
        v.setClienteId(rs.getObject("cliente_id", Integer.class));
        v.setAtencionId(rs.getObject("atencion_id", Integer.class));
        v.setMedicoId(rs.getInt("medico_id"));
        v.setMedicoNombre(joinName(rs.getString("medico_nombre"), rs.getString("medico_apellido")));
        v.setNombre(rs.getString("nombre"));
        v.setLote(rs.getString("lote"));
        v.setFechaAplicacion(isoDate(rs.getDate("fecha_aplicacion")));
        v.setProximaDosis(isoDate(rs.getDate("proxima_dosis")));
        v.setObservaciones(rs.getString("observaciones"));
        return v;
    };

    private static String isoDate(Date date) {
        return date == null ? null : date.toLocalDate().toString();
    }

    private static Date sqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private static String joinName(String nombre, String apellido) {
        if (nombre == null) {
            return apellido;
        }
        return apellido == null ? nombre : nombre + " " + apellido;
    }

    private static int requireId(Integer id) {
        if (id == null) {
            throw new IllegalStateException("La base de datos no devolvio el id generado");
        }
        return id;
    }

    @Override
    public int insertTreatment(int mascotaId, Integer atencionId, int medicoId, String medicamento, String dosis,
                               String frecuencia, String indicaciones, LocalDate fechaInicio, LocalDate fechaFin) {
        log.debug("start insertTreatment mascota={}", mascotaId);

        String sql = """
                INSERT INTO tratamiento (mascota_id, atencion_id, medico_id, medicamento, dosis, frecuencia,
                                         indicaciones, fecha_inicio, fecha_fin, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EN_CURSO')
                RETURNING tratamiento_id
                """;

        return requireId(jdbcTemplate.queryForObject(sql, Integer.class, mascotaId, atencionId, medicoId, medicamento,
                dosis, frecuencia, indicaciones, sqlDate(fechaInicio), sqlDate(fechaFin)));
    }

    @Override
    public Optional<Treatment> findTreatment(int tratamientoId) {
        return jdbcTemplate.query(SELECT_TREATMENT + " WHERE x.tratamiento_id = ?", TREATMENT_MAPPER, tratamientoId)
                .stream().findFirst();
    }

    @Override
    public List<Treatment> findTreatmentsByMascota(int mascotaId) {
        return jdbcTemplate.query(SELECT_TREATMENT + " WHERE x.mascota_id = ? ORDER BY x.fecha_inicio DESC, x.tratamiento_id DESC",
                TREATMENT_MAPPER, mascotaId);
    }

    @Override
    public int updateTreatmentStatus(int tratamientoId, TreatmentStatus currentStatus, TreatmentStatus newStatus,
                                     LocalDate fechaFin) {
        log.debug("start updateTreatmentStatus {} {} -> {}", tratamientoId, currentStatus, newStatus);

        // La fecha de fin no puede quedar antes del inicio (restricción CHECK de la tabla)
        String sql = """
                UPDATE tratamiento SET estado = ?, fecha_fin = GREATEST(?, fecha_inicio)
                WHERE tratamiento_id = ? AND estado = ?
                """;

        return jdbcTemplate.update(sql, newStatus.name(), sqlDate(fechaFin), tratamientoId, currentStatus.name());
    }

    @Override
    public int insertVaccine(int mascotaId, Integer atencionId, int medicoId, String nombre, String lote,
                             LocalDate fechaAplicacion, LocalDate proximaDosis, String observaciones) {
        log.debug("start insertVaccine mascota={}", mascotaId);

        String sql = """
                INSERT INTO vacuna (mascota_id, atencion_id, medico_id, nombre, lote, fecha_aplicacion,
                                    proxima_dosis, observaciones)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING vacuna_id
                """;

        return requireId(jdbcTemplate.queryForObject(sql, Integer.class, mascotaId, atencionId, medicoId, nombre, lote,
                sqlDate(fechaAplicacion), sqlDate(proximaDosis), observaciones));
    }

    @Override
    public Optional<Vaccine> findVaccine(int vacunaId) {
        return jdbcTemplate.query(SELECT_VACCINE + " WHERE x.vacuna_id = ?", VACCINE_MAPPER, vacunaId)
                .stream().findFirst();
    }

    @Override
    public boolean existsVaccine(int mascotaId, String nombre, LocalDate fechaAplicacion) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM vacuna
                    WHERE mascota_id = ? AND LOWER(TRIM(nombre)) = LOWER(TRIM(?)) AND fecha_aplicacion = ?
                )
                """;

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, mascotaId, nombre,
                sqlDate(fechaAplicacion)));
    }

    @Override
    public List<Vaccine> findVaccinesByMascota(int mascotaId) {
        return jdbcTemplate.query(SELECT_VACCINE + " WHERE x.mascota_id = ? ORDER BY x.fecha_aplicacion DESC, x.vacuna_id DESC",
                VACCINE_MAPPER, mascotaId);
    }

    @Override
    public List<Vaccine> findUpcomingVaccines(LocalDate hasta, Integer clienteId) {
        // Solo la última aplicación de cada vacuna: si ya se puso el refuerzo, la dosis anterior no está pendiente
        StringBuilder sql = new StringBuilder(SELECT_VACCINE).append("""
                WHERE x.proxima_dosis IS NOT NULL
                  AND x.proxima_dosis <= ?
                  AND COALESCE(m.activo, TRUE) = TRUE
                  AND NOT EXISTS (
                      SELECT 1 FROM vacuna v2
                      WHERE v2.mascota_id = x.mascota_id
                        AND LOWER(v2.nombre) = LOWER(x.nombre)
                        AND (v2.fecha_aplicacion > x.fecha_aplicacion
                             OR (v2.fecha_aplicacion = x.fecha_aplicacion AND v2.vacuna_id > x.vacuna_id))
                  )
                """);
        List<Object> params = new ArrayList<>();
        params.add(sqlDate(hasta));
        if (clienteId != null) {
            sql.append(" AND m.cliente_id = ?");
            params.add(clienteId);
        }
        sql.append(" ORDER BY x.proxima_dosis, m.nombre");

        return jdbcTemplate.query(sql.toString(), VACCINE_MAPPER, params.toArray());
    }
}
