-- =====================================================================================
-- VET-04: reprogramación de citas (y cancelación/agendamiento sin ids de estado fijos)
-- Ejecutar UNA VEZ en la base de datos (después de VET-03). Es seguro ejecutarlo de nuevo.
--
-- Desde VET-04 el backend busca los estados por su descripción:
--   - al agendar, toda cita nueva queda en el estado "programada"
--     (descripción con PROGRAM, AGENDAD o PENDIENT, pero no REPROGRAM)
--   - al cancelar, pasa al estado "cancelada" (descripción con CANCEL)
--   - al reprogramar, pasa al estado "reprogramada" (descripción con REPROGRAM)
-- =====================================================================================

BEGIN;

-- Requiere que estado_id se genere automáticamente (serial o identity)
INSERT INTO estado_cita (descripcion)
SELECT 'Reprogramada'
WHERE NOT EXISTS (SELECT 1 FROM estado_cita WHERE UPPER(descripcion) LIKE '%REPROGRAM%');

COMMIT;

-- =====================================================================================
-- Verificación: cómo reconoce el backend cada estado (mismo criterio que AppointmentStatus).
-- Deben existir al menos PROGRAMADA, CANCELADA y REPROGRAMADA; y ninguna fila debería quedar
-- como DESCONOCIDO (las citas en un estado desconocido no se pueden cancelar ni reprogramar).
-- Si un estado sale DESCONOCIDO, renombrar su descripción, por ejemplo:
--   UPDATE estado_cita SET descripcion = 'Programada' WHERE estado_id = 1;
-- =====================================================================================
SELECT estado_id,
       descripcion,
       CASE
           WHEN UPPER(descripcion) LIKE '%NO ASIST%' OR UPPER(descripcion) LIKE '%NO_ASIST%'
                OR UPPER(descripcion) LIKE '%INASIST%' THEN 'NO_ASISTIO'
           WHEN UPPER(descripcion) LIKE '%CANCEL%' THEN 'CANCELADA'
           WHEN UPPER(descripcion) LIKE '%ATENDID%' THEN 'ATENDIDA'
           WHEN UPPER(descripcion) LIKE '%CONFIRM%' THEN 'CONFIRMADA'
           WHEN UPPER(descripcion) LIKE '%REPROGRAM%' THEN 'REPROGRAMADA'
           WHEN UPPER(descripcion) LIKE '%PROGRAM%' OR UPPER(descripcion) LIKE '%AGENDAD%'
                OR UPPER(descripcion) LIKE '%PENDIENT%' THEN 'PROGRAMADA'
           ELSE 'DESCONOCIDO'
       END AS estado_reconocido
FROM estado_cita
ORDER BY estado_id;
