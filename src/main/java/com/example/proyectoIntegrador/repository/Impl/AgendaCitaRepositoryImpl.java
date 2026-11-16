package com.example.proyectoIntegrador.repository.Impl;

import com.example.proyectoIntegrador.enums.AppointmentStatus;
import com.example.proyectoIntegrador.models.AgendaCita;
import com.example.proyectoIntegrador.models.AgendaCitaDetail;
import com.example.proyectoIntegrador.models.InfoMascota;
import com.example.proyectoIntegrador.models.VeterinarianAgendaItem;
import com.example.proyectoIntegrador.repository.AgendaCitaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
public class AgendaCitaRepositoryImpl implements AgendaCitaRepository {

    private final JdbcTemplate jdbcTemplate;

    public AgendaCitaRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<List<AgendaCita>> getListAgenda(int idClient) {
        log.debug("Obtener informacion de las citas agendadas");

        try {
            // Construye la consulta SQL
            // Incluye mascota y veterinario para que el frontend no tenga que pedir el detalle de cada cita.
            // Las columnas van con alias de tabla: personal y mascota también tienen cliente_id.
            String sql = """
                    SELECT ac.agenda_id, c.descripcion, c.fecha, c.hora, ec.descripcion AS estado_cita, ac.estado_id,
                           ac.mascota_id, ma.nombre AS mascota_nombre, ac.medico_id,
                           p.nombre AS nombre_vete, p.apellido AS apellido_vete
                    FROM agenda_cita ac
                    LEFT JOIN cita c ON c.cita_id = ac.cita_id
                    LEFT JOIN estado_cita ec ON ec.estado_id = ac.estado_id
                    LEFT JOIN mascota ma ON ma.mascota_id = ac.mascota_id
                    LEFT JOIN medico m ON m.medico_id = ac.medico_id
                    LEFT JOIN personal p ON p.personal_id = m.identificacion_medico
                    WHERE ac.cliente_id = ?
                    ORDER BY c.fecha DESC, c.hora DESC
                    """;

            var empresas = jdbcTemplate.query(sql, (rs, rowNum) -> {
                AgendaCita data = new AgendaCita();
                fillAgendaCita(data, rs);
                return data;
            }, idClient);

            if (!empresas.isEmpty()) {
                return Optional.of(empresas);
            } else {
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("There was a problem listar citas agendas: " + e.getMessage());
            return Optional.empty();
        }
    }


    @Transactional
    public Optional<Integer> insertAgendaCita(String description, String fecha, LocalTime hora, Integer estadoId, Integer clienteId, Integer medicoId, Integer mascotaId) {
        log.debug("Guardar insertAgendaCita");

        // SQL para insertar la cita
        String sqlCita = """
                    INSERT INTO cita (descripcion, fecha, hora)
                    VALUES (?, ?, ?)
                """;

        // SQL para insertar en agenda_cita
        String sqlAgendaCita = """
                    INSERT INTO agenda_cita (estado_id, cliente_id, cita_id, medico_id, mascota_id)
                    VALUES (?, ?, ?, ?, ?)
                """;

        try {
            // KeyHolder para capturar el ID generado de la cita
            KeyHolder keyHolder = new GeneratedKeyHolder();

            int rowsAffected = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sqlCita, new String[]{"cita_id"});
                ps.setString(1, description);
                ps.setDate(2, Date.valueOf(obtenerFechaEnviar(fecha)));
                ps.setTime(3, java.sql.Time.valueOf(hora));
                return ps;
            }, keyHolder);

            if (rowsAffected > 0) {
                Number citaId = keyHolder.getKey();
                if (citaId == null) throw new IllegalStateException("No se pudo obtener el ID de la cita");

                // Ahora insertamos en agenda_cita usando el cita_id generado
                int agendaRows = jdbcTemplate.update(sqlAgendaCita,
                        estadoId, clienteId, citaId.intValue(), medicoId, mascotaId);

                return (agendaRows > 0) ? Optional.of(citaId.intValue()) : Optional.empty();
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Error al guardar la cita y agenda: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Boolean> validateClient(int idClient) {

        log.debug("validate client");

        String sql = """
                SELECT COUNT('x') FROM cliente WHERE cliente_id = ?
                """;

        try {
            var resp = jdbcTemplate.queryForObject(sql, Integer.class, idClient);
            return resp > 0 ? Optional.of(true) : Optional.empty();
        } catch (Exception e) {
            log.error("Error al validar cliente: " + e.getMessage());
            return Optional.empty();
        }

    }

    @Override
    public Optional<Boolean> validateVeterinarian(int idVeterinarian) {
        log.debug("validate client");

        String sql = """
                SELECT COUNT(*)
                FROM medico m
                LEFT JOIN personal p ON p.personal_id = m.identificacion_medico
                WHERE m.medico_id = ? AND COALESCE(p.activo, TRUE) = TRUE
                """;

        try {
            var resp = jdbcTemplate.queryForObject(sql, Integer.class, idVeterinarian);
            return resp > 0 ? Optional.of(true) : Optional.empty();
        } catch (Exception e) {
            log.error("Error al validar cliente: " + e.getMessage());
            return Optional.empty();
        }
    }


    @Override
    public List<VeterinarianAgendaItem> getAgendaByVeterinarian(int medicoId, LocalDate fecha, Integer estadoId) {
        log.debug("start getAgendaByVeterinarian medico={} fecha={} estado={}", medicoId, fecha, estadoId);

        // CAST: PostgreSQL necesita conocer el tipo del parámetro cuando se envía nulo
        String sql = SELECT_AGENDA_ITEM + """
                WHERE ac.medico_id = ?
                  AND c.fecha = ?
                  AND (CAST(? AS INTEGER) IS NULL OR ac.estado_id = ?)
                ORDER BY c.hora
                """;

        return jdbcTemplate.query(sql, AGENDA_ITEM_MAPPER, medicoId, Date.valueOf(fecha), estadoId, estadoId);
    }

    @Override
    public Optional<VeterinarianAgendaItem> findAgendaItem(int agendaId) {
        log.debug("start findAgendaItem {}", agendaId);

        String sql = SELECT_AGENDA_ITEM + " WHERE ac.agenda_id = ?";

        return jdbcTemplate.query(sql, AGENDA_ITEM_MAPPER, agendaId).stream().findFirst();
    }

    @Override
    public Optional<String> getEstadoDescription(int estadoId) {
        String sql = """
                SELECT descripcion FROM estado_cita WHERE estado_id = ?
                """;

        return jdbcTemplate.queryForList(sql, String.class, estadoId).stream().findFirst();
    }

    @Override
    public int updateEstado(int agendaId, int currentEstadoId, int newEstadoId) {
        log.debug("start updateEstado agenda={} {} -> {}", agendaId, currentEstadoId, newEstadoId);

        String sql = """
                UPDATE agenda_cita SET estado_id = ? WHERE agenda_id = ? AND estado_id = ?
                """;

        return jdbcTemplate.update(sql, newEstadoId, agendaId, currentEstadoId);
    }

    @Override
    public boolean isMascotaOfClient(int mascotaId, int clienteId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM mascota
                    WHERE mascota_id = ? AND cliente_id = ? AND COALESCE(activo, TRUE) = TRUE
                )
                """;

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, mascotaId, clienteId));
    }

    @Override
    public boolean isSlotAvailable(int medicoId, LocalDate fecha, LocalTime hora, Integer excludeAgendaId) {
        // Hay choque si otra cita del mismo veterinario empieza a menos de APPOINTMENT_MINUTES de esta.
        // Las canceladas no ocupan horario. Los límites no se salen del día.
        LocalTime desde = hora.isBefore(LocalTime.MIN.plusMinutes(APPOINTMENT_MINUTES))
                ? LocalTime.MIN : hora.minusMinutes(APPOINTMENT_MINUTES);
        LocalTime hasta = hora.isAfter(LocalTime.MAX.minusMinutes(APPOINTMENT_MINUTES))
                ? LocalTime.MAX : hora.plusMinutes(APPOINTMENT_MINUTES);

        String sql = """
                SELECT NOT EXISTS (
                    SELECT 1
                    FROM agenda_cita ac
                    JOIN cita c ON c.cita_id = ac.cita_id
                    LEFT JOIN estado_cita ec ON ec.estado_id = ac.estado_id
                    WHERE ac.medico_id = ?
                      AND c.fecha = ?
                      AND c.hora > ? AND c.hora < ?
                      AND UPPER(COALESCE(ec.descripcion, '')) NOT LIKE '%CANCEL%'
                      AND (CAST(? AS INTEGER) IS NULL OR ac.agenda_id <> ?)
                )
                """;

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class,
                medicoId, Date.valueOf(fecha), Time.valueOf(desde), Time.valueOf(hasta),
                excludeAgendaId, excludeAgendaId));
    }

    @Override
    public Optional<Integer> findEstadoId(AppointmentStatus status) {
        String sql = """
                SELECT estado_id, descripcion FROM estado_cita ORDER BY estado_id
                """;

        // El reconocimiento por descripción se hace en Java con el mismo criterio de AppointmentStatus
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                        AppointmentStatus.fromDescription(rs.getString("descripcion")) == status
                                ? rs.getInt("estado_id") : null)
                .stream()
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    @Override
    public int reschedule(int agendaId, int currentEstadoId, LocalDate fecha, LocalTime hora, int medicoId,
                          int newEstadoId) {
        log.debug("start reschedule agenda={} -> {} {} medico={}", agendaId, fecha, hora, medicoId);

        String sqlAgenda = """
                UPDATE agenda_cita SET medico_id = ?, estado_id = ? WHERE agenda_id = ? AND estado_id = ?
                """;
        int rows = jdbcTemplate.update(sqlAgenda, medicoId, newEstadoId, agendaId, currentEstadoId);
        if (rows == 0) {
            return 0;
        }

        String sqlCita = """
                UPDATE cita SET fecha = ?, hora = ?
                WHERE cita_id = (SELECT cita_id FROM agenda_cita WHERE agenda_id = ?)
                """;
        jdbcTemplate.update(sqlCita, Date.valueOf(fecha), Time.valueOf(hora), agendaId);

        return rows;
    }

    private static final String SELECT_AGENDA_ITEM = """
            SELECT ac.agenda_id, ac.medico_id, c.fecha, c.hora, c.descripcion, ac.estado_id, ec.descripcion AS estado,
                   cl.cliente_id, cl.nombre AS cliente_nombre, cl.apellido AS cliente_apellido,
                   cl.telefono AS cliente_telefono, m.mascota_id, m.nombre AS mascota_nombre
            FROM agenda_cita ac
            JOIN cita c ON c.cita_id = ac.cita_id
            LEFT JOIN estado_cita ec ON ec.estado_id = ac.estado_id
            LEFT JOIN cliente cl ON cl.cliente_id = ac.cliente_id
            LEFT JOIN mascota m ON m.mascota_id = ac.mascota_id
            """;

    private static final RowMapper<VeterinarianAgendaItem> AGENDA_ITEM_MAPPER = (rs, rowNum) -> {
        VeterinarianAgendaItem item = new VeterinarianAgendaItem();
        item.setAgendaId(rs.getInt("agenda_id"));
        item.setMedicoId(rs.getObject("medico_id", Integer.class));
        item.setFecha(rs.getString("fecha"));
        item.setHora(rs.getString("hora"));
        item.setDescripcion(rs.getString("descripcion"));
        item.setEstadoId(rs.getObject("estado_id", Integer.class));
        item.setEstado(rs.getString("estado"));
        item.setClienteId(rs.getObject("cliente_id", Integer.class));
        item.setClienteNombre(joinName(rs.getString("cliente_nombre"), rs.getString("cliente_apellido")));
        item.setClienteTelefono(rs.getString("cliente_telefono"));
        item.setMascotaId(rs.getObject("mascota_id", Integer.class));
        item.setMascotaNombre(rs.getString("mascota_nombre"));
        return item;
    };

    /** Columnas comunes de la lista y del detalle de citas del cliente. */
    private static void fillAgendaCita(AgendaCita data, java.sql.ResultSet rs) throws java.sql.SQLException {
        data.setAgendaCitaId(rs.getInt("agenda_id"));
        data.setDescripcion(rs.getString("descripcion"));
        data.setFechaInicio(rs.getString("fecha"));
        data.setHoraInicio(rs.getString("hora"));
        data.setEstado(rs.getString("estado_cita"));
        data.setEstadoCitaId(rs.getInt("estado_id"));
        data.setMascotaId(rs.getInt("mascota_id"));
        data.setMascotaNombre(rs.getString("mascota_nombre"));
        data.setMedicoId(rs.getObject("medico_id", Integer.class));
        // Antes era nombre + " " + apellido y sin veterinario salía "null null"
        data.setNombreVeterinario(joinName(rs.getString("nombre_vete"), rs.getString("apellido_vete")));
    }

    private static String joinName(String nombre, String apellido) {
        if (nombre == null) {
            return apellido;
        }
        return apellido == null ? nombre : nombre + " " + apellido;
    }

    @Override
    public Optional<AgendaCitaDetail> getDetailAppointment(int idAgenda, int idClient) {
        try {
            // Construye la consulta SQL
            // ac.cliente_id con alias: desde VET-01 personal también tiene cliente_id y sin alias
            // PostgreSQL rechaza la consulta por columna ambigua
            String sql = """
                    SELECT ac.agenda_id, c.descripcion, c.fecha, c.hora, ec.descripcion AS estado_cita, ac.estado_id,
                           ac.mascota_id, ma.nombre AS mascota_nombre, ac.medico_id,
                           p.nombre AS nombre_vete, p.apellido AS apellido_vete
                    FROM agenda_cita ac
                    LEFT JOIN cita c ON c.cita_id = ac.cita_id
                    LEFT JOIN estado_cita ec ON ec.estado_id = ac.estado_id
                    LEFT JOIN mascota ma ON ma.mascota_id = ac.mascota_id
                    LEFT JOIN medico m ON m.medico_id = ac.medico_id
                    LEFT JOIN personal p ON p.personal_id = m.identificacion_medico
                    WHERE ac.cliente_id = ? AND ac.agenda_id = ?
                    """;

            var empresas = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                AgendaCitaDetail data = new AgendaCitaDetail();
                fillAgendaCita(data, rs);
                return data;
            }, idClient, idAgenda);

            if (empresas != null) {
                return Optional.of(empresas);
            } else {
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("There was a problem al obtener el detalle de la citas agendada: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * @param idClient
     * @return
     */
    @Override
    public Optional<InfoMascota> getInfoMascota(int idClient, int idMascota) {
        log.debug("start getInfoMascota");

        try {
            // Construye la consulta SQL
            String sql = """
                    select m.mascota_id, m.codigo, m.nombre, m.edad, m.genero , r.descripcion as raza_mascota
                    from mascota m 
                    left join raza r on m.raza_id = r.raza_id 
                    where m.cliente_id = ? and m.mascota_id = ? 
                    """;

            var infoMascota = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                InfoMascota data = new InfoMascota();
                data.setIdMascota(rs.getInt("mascota_id"));
                data.setCodigo(rs.getString("codigo"));
                data.setNombreMascota(rs.getString("nombre"));
                data.setEdad(rs.getInt("edad"));
                data.setTipoRaza(rs.getString("raza_mascota"));
                data.setGenero(rs.getString("genero"));
                return data;
            }, idClient, idMascota);

            if (infoMascota != null) {
                return Optional.of(infoMascota);
            } else {
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("There was a problem al obtener informacion de la mascota: " + e.getMessage());
            return Optional.empty();
        }
    }


    public LocalDate obtenerFechaEnviar(String fecha) {

        if (fecha != null && !fecha.isEmpty()) {

            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Convertir a LocalDate
            LocalDate date = LocalDate.parse(fecha, inputFormatter);

            // Convertir a String con el formato deseadO
            return date;

        } else {
            // Obtener la fecha de hoy
            LocalDate today = LocalDate.now();

            // Crear un formateador para el nuevo formato
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");  // Cambiar a formato yyyyMMdd

            // Formatear la fecha en el nuevo formato
            return LocalDate.parse(today.format(formatter), formatter);
        }

    }

}
