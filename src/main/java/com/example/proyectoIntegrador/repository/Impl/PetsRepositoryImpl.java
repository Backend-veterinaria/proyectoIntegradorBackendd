package com.example.proyectoIntegrador.repository.Impl;

import com.example.proyectoIntegrador.models.InfoMascota;
import com.example.proyectoIntegrador.repository.PetsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
@Slf4j
@RequiredArgsConstructor
public class PetsRepositoryImpl implements PetsRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param petName
     * @return
     */
    @Override
    public Optional<Boolean> validateExistPet(String nombre, int edad, String genero, Integer clienteId) {
        String sql = """
                    SELECT COUNT(*) FROM mascota
                    WHERE nombre = ? AND edad = ? AND genero = ? AND cliente_id = ?
                """;

        try {
            Integer count = jdbcTemplate.queryForObject(
                    sql,
                    Integer.class,
                    nombre.trim(), edad, genero.trim(), clienteId
            );

            return Optional.of(count > 0);
        } catch (Exception e) {
            log.error("Error al validar la mascota: {}", e.getMessage(), e);
            return Optional.empty();
        }

    }

    /**
     * @param clientId
     * @param petName
     * @param petAge
     * @param petGender
     * @param razaId
     * @return
     */
    @Override
    public Optional<Boolean> addPet(Integer clientId, String petCodigo, String petName, Integer petAge, String petGender, Integer razaId) {

        log.debug("Guardando pet");

        // SQL para insertar en agenda_cita
        String sqlAgendaCita = """
                INSERT INTO mascota
                (codigo, nombre, edad, cliente_id, raza_id, genero)
                VALUES( ?, ?, ?, ?, ?, ?);
                """;

        try {

            // Ahora insertamos en agenda_cita usando el cita_id generado
            int agendaRows = jdbcTemplate.update(sqlAgendaCita, petCodigo,
                    petName, petAge, clientId, razaId, petGender);

            return (agendaRows > 0) ? Optional.of(true) : Optional.empty();

        } catch (Exception e) {
            log.error("Error al guardar la mascota: {}", e.getMessage(), e);
            return Optional.empty();
        }

    }

    private static final String SELECT_PET = """
            SELECT m.mascota_id, m.codigo, m.nombre, m.edad, m.genero, m.raza_id, m.cliente_id,
                   COALESCE(m.activo, TRUE) AS activo, r.descripcion AS raza_mascota
            FROM mascota m
            LEFT JOIN raza r ON m.raza_id = r.raza_id
            """;

    private static final RowMapper<InfoMascota> PET_MAPPER = (rs, rowNum) -> {
        InfoMascota data = new InfoMascota();
        data.setIdMascota(rs.getInt("mascota_id"));
        data.setCodigo(rs.getString("codigo"));
        data.setNombreMascota(rs.getString("nombre"));
        data.setEdad(rs.getInt("edad"));
        data.setTipoRaza(rs.getString("raza_mascota"));
        data.setGenero(rs.getString("genero"));
        data.setRazaId(rs.getObject("raza_id", Integer.class));
        data.setClienteId(rs.getObject("cliente_id", Integer.class));
        data.setActivo(rs.getBoolean("activo"));
        return data;
    };

    @Override
    public Optional<List<InfoMascota>> getListPetsByUser(int idClient, boolean includeInactive) {

        try {
            String sql = SELECT_PET + """
                    WHERE m.cliente_id = ? AND (? OR COALESCE(m.activo, TRUE) = TRUE)
                    ORDER BY m.nombre
                    """;

            var infoMascota = jdbcTemplate.query(sql, PET_MAPPER, idClient, includeInactive);

            return infoMascota.isEmpty() ? Optional.empty() : Optional.of(infoMascota);

        } catch (Exception e) {
            log.error("Error al listar las mascotas del cliente {}: {}", idClient, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<InfoMascota> findById(int mascotaId) {
        return jdbcTemplate.query(SELECT_PET + " WHERE m.mascota_id = ?", PET_MAPPER, mascotaId)
                .stream().findFirst();
    }

    @Override
    public boolean existsOtherPet(String nombre, int edad, String genero, int clienteId, int excludeMascotaId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM mascota
                    WHERE nombre = ? AND edad = ? AND genero = ? AND cliente_id = ? AND mascota_id <> ?
                )
                """;

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class,
                nombre.trim(), edad, genero.trim(), clienteId, excludeMascotaId));
    }

    @Override
    public int updatePet(int mascotaId, String nombre, int edad, String genero, int razaId) {
        String sql = """
                UPDATE mascota SET nombre = ?, edad = ?, genero = ?, raza_id = ? WHERE mascota_id = ?
                """;

        return jdbcTemplate.update(sql, nombre.trim(), edad, genero.trim(), razaId, mascotaId);
    }

    @Override
    public int updateStatus(int mascotaId, boolean activo) {
        return jdbcTemplate.update("UPDATE mascota SET activo = ? WHERE mascota_id = ?", activo, mascotaId);
    }
}
