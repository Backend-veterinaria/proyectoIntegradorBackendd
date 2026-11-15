package com.example.proyectoIntegrador.repository.Impl;

import com.example.proyectoIntegrador.models.VeterinarianInfo;
import com.example.proyectoIntegrador.repository.VeterinarianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class VeterinarianRepositoryImpl implements VeterinarianRepository {

    private static final String SELECT_VETERINARIAN = """
            SELECT m.medico_id, p.personal_id, p.nombre, p.apellido, p.activo,
                   c.cliente_id, c.username, c.email, c.telefono, c.direccion
            FROM medico m
            JOIN personal p ON p.personal_id = m.identificacion_medico
            LEFT JOIN cliente c ON c.cliente_id = p.cliente_id
            """;

    private static final RowMapper<VeterinarianInfo> ROW_MAPPER = (rs, rowNum) -> {
        VeterinarianInfo data = new VeterinarianInfo();
        data.setMedicoId(rs.getInt("medico_id"));
        data.setPersonalId(rs.getInt("personal_id"));
        data.setClienteId(rs.getObject("cliente_id", Integer.class));
        data.setNombre(rs.getString("nombre"));
        data.setApellido(rs.getString("apellido"));
        data.setUsername(rs.getString("username"));
        data.setEmail(rs.getString("email"));
        data.setTelefono(rs.getString("telefono"));
        data.setDireccion(rs.getString("direccion"));
        data.setActivo(rs.getBoolean("activo"));
        return data;
    };

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<Integer> insertPersonal(String nombre, String apellido, int rolId, int clienteId) {
        log.debug("start insertPersonal");

        String sql = """
                INSERT INTO personal (nombre, apellido, rol_id, cliente_id, activo)
                VALUES (?, ?, ?, ?, TRUE)
                RETURNING personal_id
                """;

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, Integer.class, nombre, apellido, rolId, clienteId));
        } catch (Exception e) {
            log.error("No se pudo insertar el personal: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Integer> insertMedico(int personalId) {
        log.debug("start insertMedico");

        String sql = """
                INSERT INTO medico (identificacion_medico) VALUES (?) RETURNING medico_id
                """;

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, Integer.class, personalId));
        } catch (Exception e) {
            log.error("No se pudo insertar el medico: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<VeterinarianInfo> findAll(boolean includeInactive) {
        log.debug("start findAll veterinarians, includeInactive={}", includeInactive);

        String sql = SELECT_VETERINARIAN + """
                WHERE (? OR p.activo = TRUE)
                ORDER BY p.apellido, p.nombre
                """;

        return jdbcTemplate.query(sql, ROW_MAPPER, includeInactive);
    }

    @Override
    public Optional<VeterinarianInfo> findById(int medicoId) {
        log.debug("start findById veterinarian {}", medicoId);

        String sql = SELECT_VETERINARIAN + " WHERE m.medico_id = ?";

        return jdbcTemplate.query(sql, ROW_MAPPER, medicoId).stream().findFirst();
    }

    @Override
    public Optional<Boolean> updatePersonal(int personalId, String nombre, String apellido) {
        log.debug("start updatePersonal {}", personalId);

        String sql = """
                UPDATE personal SET nombre = ?, apellido = ? WHERE personal_id = ?
                """;

        try {
            int rows = jdbcTemplate.update(sql, nombre, apellido, personalId);
            return rows > 0 ? Optional.of(true) : Optional.empty();
        } catch (Exception e) {
            log.error("No se pudo actualizar el personal {}: {}", personalId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Boolean> updateUser(int clienteId, String nombre, String apellido, String telefono, String email,
                                        String direccion) {
        log.debug("start updateUser {}", clienteId);

        String sql = """
                UPDATE cliente
                SET nombre = ?, apellido = ?, telefono = ?, email = ?, direccion = COALESCE(?, direccion)
                WHERE cliente_id = ?
                """;

        try {
            int rows = jdbcTemplate.update(sql, nombre, apellido, telefono, email, direccion, clienteId);
            return rows > 0 ? Optional.of(true) : Optional.empty();
        } catch (Exception e) {
            log.error("No se pudo actualizar el usuario {}: {}", clienteId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Boolean> updateStatus(int personalId, boolean activo) {
        log.debug("start updateStatus personal {} activo={}", personalId, activo);

        String sql = """
                UPDATE personal SET activo = ? WHERE personal_id = ?
                """;

        try {
            int rows = jdbcTemplate.update(sql, activo, personalId);
            return rows > 0 ? Optional.of(true) : Optional.empty();
        } catch (Exception e) {
            log.error("No se pudo cambiar el estado del personal {}: {}", personalId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean existsEmailForOtherUser(String email, Integer clienteId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM cliente
                    WHERE LOWER(email) = LOWER(?) AND cliente_id IS DISTINCT FROM ?
                )
                """;

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, email, clienteId));
    }

    @Override
    public boolean isStaffActive(int clienteId) {
        // BOOL_AND sin filas devuelve NULL: un usuario que no es personal no queda bloqueado
        String sql = """
                SELECT COALESCE(BOOL_AND(activo), TRUE) FROM personal WHERE cliente_id = ?
                """;

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, clienteId));
    }
}
