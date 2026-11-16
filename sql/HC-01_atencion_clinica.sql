-- =====================================================================================
-- HC-01: registro de la atención clínica (RF-09)
-- Ejecutar UNA VEZ en la base de datos antes de usar /integrador/clinicalHistory.
-- Es seguro ejecutarlo de nuevo: usa IF NOT EXISTS.
-- =====================================================================================

BEGIN;

-- Una atención por cita. Forma parte de la historia clínica de la mascota, que es privada y
-- está sometida a reserva (Ley 576 de 2000, art. 61): no se borra ni se edita desde la API.
CREATE TABLE IF NOT EXISTS atencion_clinica (
    atencion_id     SERIAL PRIMARY KEY,
    agenda_id       INTEGER NOT NULL UNIQUE REFERENCES agenda_cita (agenda_id),
    mascota_id      INTEGER NOT NULL REFERENCES mascota (mascota_id),
    medico_id       INTEGER NOT NULL REFERENCES medico (medico_id),
    fecha_atencion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo_consulta VARCHAR(500),
    diagnostico     TEXT NOT NULL,
    procedimientos  TEXT,
    observaciones   TEXT,
    -- Signos vitales opcionales
    peso_kg         NUMERIC(6, 2) CHECK (peso_kg > 0),
    temperatura_c   NUMERIC(4, 1) CHECK (temperatura_c BETWEEN 30 AND 45)
);

-- El historial de una mascota se consulta por mascota y fecha (HC-03)
CREATE INDEX IF NOT EXISTS ix_atencion_clinica_mascota ON atencion_clinica (mascota_id, fecha_atencion DESC);

COMMIT;

-- =====================================================================================
-- Verificación (solo lectura)
-- =====================================================================================
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'atencion_clinica'
ORDER BY ordinal_position;
