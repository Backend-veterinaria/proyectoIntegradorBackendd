-- Datos mínimos para las pruebas de integración (se cargan después de sql/schema.sql).
-- Los ids son a propósito distintos de los de la base real: el código no debe depender de ellos.

INSERT INTO rol (rol_id, descripcion) VALUES (10, 'Administrador'), (20, 'Veterinario'), (30, 'Cliente');
SELECT setval('rol_rol_id_seq', 30);

INSERT INTO estado_cita (estado_id, descripcion) VALUES
    (11, 'Programada'), (12, 'Reprogramada'), (13, 'Cancelada'),
    (14, 'Confirmada'), (15, 'Atendida'), (16, 'No asistio');

INSERT INTO raza (raza_id, descripcion) VALUES (1, 'Labrador'), (2, 'Criollo');
SELECT setval('raza_raza_id_seq', 2);
