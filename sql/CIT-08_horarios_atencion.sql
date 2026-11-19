-- =====================================================================================
-- CIT-08: horarios de atención y parámetros del sistema, configurables desde la base de datos
-- Ejecutar UNA VEZ. Es seguro ejecutarlo de nuevo: usa IF NOT EXISTS y solo carga los datos de
-- ejemplo si las tablas están vacías.
-- =====================================================================================

BEGIN;

-- Parámetros generales (clave/valor). Se cambian con UPDATE o con PUT /integrador/parameters/{clave}.
CREATE TABLE IF NOT EXISTS parametro_sistema (
    clave       VARCHAR(100) PRIMARY KEY,
    valor       VARCHAR(200) NOT NULL,
    descripcion VARCHAR(500)
);

INSERT INTO parametro_sistema (clave, valor, descripcion)
VALUES ('duracion_cita_minutos', '30',
        'Duración de una cita en minutos: cada cuánto se ofrecen horarios y separación mínima entre dos citas del mismo veterinario (entre 5 y 240)')
ON CONFLICT (clave) DO NOTHING;

-- Franjas de atención. medico_id NULL = horario general de la clínica; con valor = horario propio de
-- ese veterinario, que reemplaza al general los días en que tenga franjas. Un día sin franjas = cerrado.
-- dia_semana: 1 = lunes ... 7 = domingo.
CREATE TABLE IF NOT EXISTS horario_atencion (
    horario_id  SERIAL PRIMARY KEY,
    medico_id   INTEGER REFERENCES medico (medico_id),
    dia_semana  SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    hora_inicio TIME NOT NULL,
    hora_fin    TIME NOT NULL,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (hora_fin > hora_inicio)
);
CREATE INDEX IF NOT EXISTS ix_horario_atencion_dia ON horario_atencion (dia_semana, medico_id);

-- Horario general de EJEMPLO (cambiarlo según la veterinaria):
-- lunes a viernes 08:00-12:00 y 14:00-18:00, sábados 08:00-12:00, domingos cerrado.
INSERT INTO horario_atencion (medico_id, dia_semana, hora_inicio, hora_fin)
SELECT NULL, d, h.ini, h.fin
FROM generate_series(1, 5) AS d,
     (VALUES (TIME '08:00', TIME '12:00'), (TIME '14:00', TIME '18:00')) AS h(ini, fin)
WHERE NOT EXISTS (SELECT 1 FROM horario_atencion)
UNION ALL
SELECT NULL, 6, TIME '08:00', TIME '12:00'
WHERE NOT EXISTS (SELECT 1 FROM horario_atencion);

COMMIT;

-- Ejemplo: el veterinario con medico_id 2 solo atiende los martes en la tarde
-- INSERT INTO horario_atencion (medico_id, dia_semana, hora_inicio, hora_fin) VALUES (2, 2, '14:00', '18:00');

-- Verificación
SELECT COALESCE(medico_id::text, 'clínica') AS de, dia_semana, hora_inicio, hora_fin, activo
FROM horario_atencion ORDER BY medico_id NULLS FIRST, dia_semana, hora_inicio;
SELECT * FROM parametro_sistema;
