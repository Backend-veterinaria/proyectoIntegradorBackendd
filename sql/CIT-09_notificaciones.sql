-- =====================================================================================
-- CIT-09: notificaciones guardadas y preferencias por usuario, configurables desde la base de datos
-- Ejecutar UNA VEZ. Es seguro ejecutarlo de nuevo: usa IF NOT EXISTS y ON CONFLICT.
-- Sin este script los avisos se siguen enviando en tiempo real, pero no se guardan.
-- =====================================================================================

BEGIN;

-- Catálogo de tipos de aviso. El código es el nombre del evento SSE. Con activo = FALSE un tipo deja de
-- enviarse y guardarse para todos. Se pueden agregar tipos nuevos sin tocar código (el backend debe
-- generar ese evento).
CREATE TABLE IF NOT EXISTS tipo_notificacion (
    codigo      VARCHAR(50) PRIMARY KEY,
    titulo      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(300),
    activo      BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO tipo_notificacion (codigo, titulo, descripcion) VALUES
    ('cita-creada', 'Cita agendada', 'Cuando se agenda una cita'),
    ('cita-cancelada', 'Cita cancelada', 'Cuando se cancela una cita'),
    ('cita-reprogramada', 'Cita reprogramada', 'Cuando una cita cambia de fecha, hora o veterinario')
ON CONFLICT (codigo) DO NOTHING;

-- Avisos guardados de cada usuario (cliente o personal)
CREATE TABLE IF NOT EXISTS notificacion (
    notificacion_id SERIAL PRIMARY KEY,
    cliente_id      INTEGER NOT NULL REFERENCES cliente (cliente_id),
    tipo            VARCHAR(50) NOT NULL REFERENCES tipo_notificacion (codigo),
    titulo          VARCHAR(100) NOT NULL,
    mensaje         VARCHAR(500) NOT NULL,
    datos           TEXT,
    leida           BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS ix_notificacion_usuario ON notificacion (cliente_id, fecha_creacion DESC);

-- Qué tipos quiere recibir cada usuario. Si no tiene fila para un tipo, lo recibe.
CREATE TABLE IF NOT EXISTS preferencia_notificacion (
    cliente_id INTEGER NOT NULL REFERENCES cliente (cliente_id),
    tipo       VARCHAR(50) NOT NULL REFERENCES tipo_notificacion (codigo),
    activa     BOOLEAN NOT NULL,
    PRIMARY KEY (cliente_id, tipo)
);

COMMIT;

-- Verificación
SELECT * FROM tipo_notificacion ORDER BY codigo;
