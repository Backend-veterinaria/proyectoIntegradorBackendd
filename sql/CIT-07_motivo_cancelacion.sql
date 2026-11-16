-- =====================================================================================
-- CIT-07: motivo de cancelación de las citas
-- Ejecutar UNA VEZ. Es seguro ejecutarlo de nuevo: usa IF NOT EXISTS.
-- Después de hacer git pull hay que ejecutarlo: la lista, el detalle y la agenda de citas lo leen.
-- =====================================================================================

-- Opcional: quien cancela puede dejar el motivo (por ejemplo, "La mascota ya se recuperó").
ALTER TABLE agenda_cita ADD COLUMN IF NOT EXISTS motivo_cancelacion VARCHAR(500);

-- Verificación
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'agenda_cita' AND column_name = 'motivo_cancelacion';
