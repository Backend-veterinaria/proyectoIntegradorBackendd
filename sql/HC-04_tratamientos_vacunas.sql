-- =====================================================================================
-- HC-04 / HC-05: tratamientos y vacunas (RF-11)
-- Ejecutar UNA VEZ, después de sql/HC-01_atencion_clinica.sql.
-- Es seguro ejecutarlo de nuevo: usa IF NOT EXISTS.
-- =====================================================================================

BEGIN;

-- Tratamientos formulados a una mascota, opcionalmente dentro de una atención clínica.
-- Se hacen seguimiento con su estado: EN_CURSO -> FINALIZADO o SUSPENDIDO.
CREATE TABLE IF NOT EXISTS tratamiento (
    tratamiento_id  SERIAL PRIMARY KEY,
    mascota_id      INTEGER NOT NULL REFERENCES mascota (mascota_id),
    atencion_id     INTEGER REFERENCES atencion_clinica (atencion_id),
    medico_id       INTEGER NOT NULL REFERENCES medico (medico_id),
    medicamento     VARCHAR(200) NOT NULL,
    dosis           VARCHAR(200) NOT NULL,
    frecuencia      VARCHAR(200),
    indicaciones    TEXT,
    fecha_inicio    DATE NOT NULL,
    fecha_fin       DATE,
    estado          VARCHAR(20) NOT NULL DEFAULT 'EN_CURSO'
                    CHECK (estado IN ('EN_CURSO', 'FINALIZADO', 'SUSPENDIDO')),
    fecha_registro  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);
CREATE INDEX IF NOT EXISTS ix_tratamiento_mascota ON tratamiento (mascota_id, fecha_inicio DESC);

-- Vacunas aplicadas. proxima_dosis permite el seguimiento de las vacunas pendientes.
CREATE TABLE IF NOT EXISTS vacuna (
    vacuna_id        SERIAL PRIMARY KEY,
    mascota_id       INTEGER NOT NULL REFERENCES mascota (mascota_id),
    atencion_id      INTEGER REFERENCES atencion_clinica (atencion_id),
    medico_id        INTEGER NOT NULL REFERENCES medico (medico_id),
    nombre           VARCHAR(200) NOT NULL,
    lote             VARCHAR(100),
    fecha_aplicacion DATE NOT NULL,
    proxima_dosis    DATE,
    observaciones    TEXT,
    fecha_registro   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (proxima_dosis IS NULL OR proxima_dosis > fecha_aplicacion)
);
CREATE INDEX IF NOT EXISTS ix_vacuna_mascota ON vacuna (mascota_id, fecha_aplicacion DESC);
CREATE INDEX IF NOT EXISTS ix_vacuna_proxima_dosis ON vacuna (proxima_dosis) WHERE proxima_dosis IS NOT NULL;

COMMIT;

-- =====================================================================================
-- Verificación (solo lectura)
-- =====================================================================================
SELECT table_name, COUNT(*) AS columnas
FROM information_schema.columns
WHERE table_name IN ('tratamiento', 'vacuna')
GROUP BY table_name;
