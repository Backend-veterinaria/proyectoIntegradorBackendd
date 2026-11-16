package com.example.proyectoIntegrador.repository;

import com.example.proyectoIntegrador.enums.TreatmentStatus;
import com.example.proyectoIntegrador.models.Treatment;
import com.example.proyectoIntegrador.models.Vaccine;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Tratamientos y vacunas (HC-04, HC-05). Requiere sql/HC-04_tratamientos_vacunas.sql.
 * Los métodos de escritura no capturan excepciones.
 */
public interface TreatmentVaccineRepository {

    int insertTreatment(int mascotaId, Integer atencionId, int medicoId, String medicamento, String dosis,
                        String frecuencia, String indicaciones, LocalDate fechaInicio, LocalDate fechaFin);

    Optional<Treatment> findTreatment(int tratamientoId);

    /** Tratamientos de una mascota, del más reciente al más antiguo. */
    List<Treatment> findTreatmentsByMascota(int mascotaId);

    /**
     * Cambia el estado solo si sigue en el estado esperado, y pone la fecha de fin.
     *
     * @return filas actualizadas: 0 si el tratamiento ya no estaba en currentStatus
     */
    int updateTreatmentStatus(int tratamientoId, TreatmentStatus currentStatus, TreatmentStatus newStatus, LocalDate fechaFin);

    int insertVaccine(int mascotaId, Integer atencionId, int medicoId, String nombre, String lote,
                      LocalDate fechaAplicacion, LocalDate proximaDosis, String observaciones);

    Optional<Vaccine> findVaccine(int vacunaId);

    /** Indica si la mascota ya tiene esa vacuna (nombre sin importar mayúsculas) aplicada en esa fecha. */
    boolean existsVaccine(int mascotaId, String nombre, LocalDate fechaAplicacion);

    /** Vacunas de una mascota, de la más reciente a la más antigua. */
    List<Vaccine> findVaccinesByMascota(int mascotaId);

    /**
     * Seguimiento de vacunas (HC-05): última dosis de cada vacuna de las mascotas activas cuya
     * próxima dosis vence hasta {@code hasta} (incluye las vencidas), de la más próxima a la más lejana.
     *
     * @param clienteId si no es nulo, solo las mascotas de ese cliente
     */
    List<Vaccine> findUpcomingVaccines(LocalDate hasta, Integer clienteId);
}
