package com.example.proyectoIntegrador.repository.Impl;

import com.example.proyectoIntegrador.models.InfoUser;
import com.example.proyectoIntegrador.models.RegisterDTO;
import com.example.proyectoIntegrador.repository.ProyectoRepository;
import com.example.proyectoIntegrador.security.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Slf4j
public class ProyectoRepositoryImpl implements ProyectoRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProyectoRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Integer> registerUser(RegisterDTO registerDTO, int rolId) {
        log.debug("start registerUser");

        try {

            String sql = "INSERT INTO cliente (nombre, apellido, direccion, telefono, email, fecha_registro, estado_cliente, username, rol_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            return Optional.of(jdbcTemplate.update(sql,
                    registerDTO.getNombre(),
                    registerDTO.getApellido(),
                    registerDTO.getDireccion(),
                    registerDTO.getTelefono(),
                    registerDTO.getEmail(),
                    registerDTO.getFechaRegistro(),
                    true,
                    registerDTO.getUsername(),
                    rolId
            ));

        } catch (Exception e) {
            log.debug("No se pudo registar el usuario: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Integer> getRoleId(Role role) {
        log.debug("start getRoleId {}", role);

        // Mismo criterio de Role.fromDescription: se busca por la descripción, no por un id fijo
        String pattern = switch (role) {
            case ADMIN -> "%ADMIN%";
            case VETERINARIO -> "%VETERIN%";
            case CLIENTE -> "%CLIENT%";
        };

        String sql = """
                SELECT rol_id FROM rol WHERE UPPER(descripcion) LIKE ? ORDER BY rol_id LIMIT 1
                """;

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, Integer.class, pattern));
        } catch (Exception e) {
            log.error("No se encontro el rol {} en la tabla rol: {}", role, e.getMessage());
            return Optional.empty();
        }
    }


    @Override
    public boolean existsUsernameOrEmail(String username, String email) {
        log.debug("start existsUsernameOrEmail");

        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM cliente
                    WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?)
                )
                """;

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, username, email));
    }


    @Override
    public boolean isUserActive(int clienteId) {
        String sql = """
                SELECT COALESCE(estado_cliente, TRUE) FROM cliente WHERE cliente_id = ?
                """;

        return jdbcTemplate.queryForList(sql, Boolean.class, clienteId).stream()
                .findFirst()
                .orElse(false);
    }

    @Override
    public boolean mustChangePassword(int clienteId) {
        String sql = """
                SELECT cambiar_password FROM cliente WHERE cliente_id = ?
                """;

        try {
            return jdbcTemplate.queryForList(sql, Boolean.class, clienteId).stream()
                    .findFirst()
                    .map(Boolean.TRUE::equals)
                    .orElse(false);
        } catch (Exception e) {
            log.warn("No se pudo consultar cliente.cambiar_password (¿se ejecutó sql/CIT-06?): {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int setMustChangePassword(int clienteId, boolean value) {
        return jdbcTemplate.update("UPDATE cliente SET cambiar_password = ? WHERE cliente_id = ?", value, clienteId);
    }


    @Override
    public Optional<Integer> getIdUser(String userName) {
        log.debug("Obtener informacion del cliente");

        try {
            String sql = """
                    SELECT cliente_id FROM cliente WHERE username = ?
                    """;

            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, Integer.class, userName));

        } catch (Exception e) {
            log.debug("No se pudo encontro usuario con ese username: {}", e.getMessage());
            return Optional.empty();
        }

    }


    @Override
    public Optional<InfoUser> validateUser(String username) {

        log.debug("Obtener informacion del cliente");

        try {

            String sql = """
                    SELECT c.cliente_id, c.nombre, c.apellido, c.direccion, c.telefono, c.email, c.fecha_registro,
                           c.estado_cliente, c.username, r.descripcion AS rol_descripcion
                    FROM cliente c
                    LEFT JOIN rol r ON r.rol_id = c.rol_id
                    WHERE c.username = ?
                    """;
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                InfoUser data = new InfoUser();
                data.setIdClient(rs.getString("cliente_id"));
                data.setName(rs.getString("nombre"));
                data.setLastName(rs.getString("apellido"));
                data.setAddres(rs.getString("direccion"));
                data.setPhone(rs.getString("telefono"));
                data.setEmail(rs.getString("email"));
                data.setUsername(rs.getString("username"));
                data.setRole(Role.fromDescription(rs.getString("rol_descripcion")).name());
                return data;
            }, username));

        } catch (Exception e) {
            log.debug("No se pudo validar el usuario: {}", e.getMessage());
            return Optional.empty();
        }

    }

    @Override
    public Optional<String> getPasswordHash(int userId) {
        log.debug("start getPasswordHash");

        String sql = """
                SELECT password_hash FROM user_credentials WHERE usuario_id = ?
                """;

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, String.class, userId));
        } catch (Exception e) {
            log.error("Error obteniendo la contraseña del usuario {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Integer> updatePasswordHash(int userId, String passwordHash) {
        log.debug("start updatePasswordHash");

        String sql = """
                UPDATE user_credentials SET password_hash = ? WHERE usuario_id = ?
                """;

        try {
            return Optional.of(jdbcTemplate.update(sql, passwordHash, userId));
        } catch (Exception e) {
            log.error("Error actualizando la contraseña del usuario {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }


    @Override
    public Optional<Integer> savePasswordUser(int idClient, String password) {

        log.debug("start guardar password");
        try {
            String sql = """
                    INSERT INTO user_credentials (usuario_id, password_hash) VALUES (?, ?)
                    """;

            return Optional.of(jdbcTemplate.update(sql,
                    idClient,
                    password
            ));

        } catch (Exception e) {
            log.debug("No se pudo guardar la contraseña: {}", e.getMessage());
            return Optional.empty();
        }

    }


    @Override
    public Optional<Integer> setSessiones(int codeSession, int userId) {

        log.debug("start guardar session");
        try {
            String sql = """
                    INSERT INTO sesiones (usuario_id, token_sesion, fecha_inicio, estado) VALUES (?, ?, CURRENT_TIMESTAMP, ?)
                    """;

            return Optional.of(jdbcTemplate.update(sql,
                    userId,
                    codeSession,
                    "ACTIVA"
            ));

        } catch (Exception e) {
            log.debug("No se pudo guardar session: {}", e.getMessage());
            return Optional.empty();
        }

    }
}
