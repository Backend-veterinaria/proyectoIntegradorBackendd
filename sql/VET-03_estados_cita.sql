-- =====================================================================================
-- VET-03: cambio de estado de las citas (confirmada, atendida, no asistió)
-- Ejecutar UNA VEZ en la base de datos. Es seguro ejecutarlo de nuevo: solo inserta los
-- estados que no existan.
--
-- El backend reconoce cada estado por su descripción (no por su id):
--   contiene "CONFIRM"            -> confirmada
--   contiene "ATENDID"            -> atendida
--   contiene "NO ASIST"/"INASIST" -> no asistió
--   contiene "CANCEL"             -> cancelada
--   contiene "REPROGRAM"          -> reprogramada
--   contiene "PROGRAM"/"AGENDAD"/"PENDIENT" -> programada
-- =====================================================================================

-- 0. Revisar los estados actuales
SELECT estado_id, descripcion FROM estado_cita ORDER BY estado_id;

BEGIN;

-- Requiere que estado_id se genere automáticamente (serial o identity)
INSERT INTO estado_cita (descripcion)
SELECT 'Confirmada'
WHERE NOT EXISTS (SELECT 1 FROM estado_cita WHERE UPPER(descripcion) LIKE '%CONFIRM%');

INSERT INTO estado_cita (descripcion)
SELECT 'Atendida'
WHERE NOT EXISTS (SELECT 1 FROM estado_cita WHERE UPPER(descripcion) LIKE '%ATENDID%');

INSERT INTO estado_cita (descripcion)
SELECT 'No asistio'
WHERE NOT EXISTS (SELECT 1 FROM estado_cita
                  WHERE UPPER(descripcion) LIKE '%NO ASIST%' OR UPPER(descripcion) LIKE '%INASIST%');

COMMIT;

-- 1. Verificar: cada estado debe tener una descripción reconocible
SELECT estado_id, descripcion FROM estado_cita ORDER BY estado_id;
