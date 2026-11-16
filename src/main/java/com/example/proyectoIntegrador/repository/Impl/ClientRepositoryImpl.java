package com.example.proyectoIntegrador.repository.Impl;

import com.example.proyectoIntegrador.models.ClientInfo;
import com.example.proyectoIntegrador.repository.ClientRepository;
import com.example.proyectoIntegrador.security.Role;
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
public class ClientRepositoryImpl implements ClientRepository {

    private static final String SELECT_CLIENT = """
            SELECT c.cliente_id, c.nombre, c.apellido, c.direccion, c.telefono, c.email, c.username,
                   c.fecha_registro, COALESCE(c.estado_cliente, TRUE) AS activo, r.descripcion AS rol_descripcion
            FROM cliente c
            LEFT JOIN rol r ON r.rol_id = c.rol_id
            """;

    private static final RowMapper<ClientInfo> CLIENT_MAPPER = (rs, rowNum) -> {
        ClientInfo data = new ClientInfo();
        data.setClienteId(rs.getInt("cliente_id"));
        data.setNombre(rs.getString("nombre"));
        data.setApellido(rs.getString("apellido"));
        data.setDireccion(rs.getString("direccion"));
        data.setTelefono(rs.getString("telefono"));
        data.setEmail(rs.getString("email"));
        data.setUsername(rs.getString("username"));
        data.setFechaRegistro(rs.getString("fecha_registro"));
        data.setActivo(rs.getBoolean("activo"));
        data.setRole(Role.fromDescription(rs.getString("rol_descripcion")).name());
        return data;
    };

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<ClientInfo> findById(int clienteId) {
        return jdbcTemplate.query(SELECT_CLIENT + " WHERE c.cliente_id = ?", CLIENT_MAPPER, clienteId)
                .stream().findFirst();
    }

    @Override
    public List<ClientInfo> findOwners(boolean includeInactive, String search) {
        // Mismo criterio de Role.fromDescription: el personal (ADMIN / VETERIN / MEDIC) no es propietario
        String sql = SELECT_CLIENT + """
                WHERE UPPER(COALESCE(r.descripcion, '')) NOT LIKE '%ADMIN%'
                  AND UPPER(COALESCE(r.descripcion, '')) NOT LIKE '%VETERIN%'
                  AND UPPER(COALESCE(r.descripcion, '')) NOT LIKE '%MEDIC%'
                  AND UPPER(COALESCE(r.descripcion, '')) NOT LIKE '%MÉDIC%'
                  AND (? OR COALESCE(c.estado_cliente, TRUE) = TRUE)
                  AND (CAST(? AS TEXT) IS NULL
                       OR LOWER(c.nombre || ' ' || c.apellido) LIKE ?
                       OR LOWER(c.username) LIKE ?
                       OR LOWER(c.email) LIKE ?)
                ORDER BY c.apellido, c.nombre
                """;

        String term = (search == null || search.isBlank()) ? null : search.trim().toLowerCase();
        String like = term == null ? null : "%" + escapeLike(term) + "%";

        return jdbcTemplate.query(sql, CLIENT_MAPPER, includeInactive, term, like, like, like);
    }

    /** Evita que % y _ escritos por el usuario actúen como comodines. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    public boolean existsEmailForOtherUser(String email, int clienteId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM cliente WHERE LOWER(email) = LOWER(?) AND cliente_id <> ?
                )
                """;

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, email, clienteId));
    }

    @Override
    public int update(int clienteId, String nombre, String apellido, String direccion, String telefono, String email) {
        log.debug("start update cliente {}", clienteId);

        String sql = """
                UPDATE cliente SET nombre = ?, apellido = ?, direccion = ?, telefono = ?, email = ?
                WHERE cliente_id = ?
                """;

        return jdbcTemplate.update(sql, nombre, apellido, direccion, telefono, email, clienteId);
    }

    @Override
    public int updateStatus(int clienteId, boolean activo) {
        log.debug("start updateStatus cliente {} activo={}", clienteId, activo);
        return jdbcTemplate.update("UPDATE cliente SET estado_cliente = ? WHERE cliente_id = ?", activo, clienteId);
    }
}
