-- =====================================================================================
-- SOLO DESARROLLO: crea un usuario para iniciar sesión a cada veterinario que no tiene uno
-- (los veterinarios de ejemplo, como Laura Gomez o Carlos Perez, se crearon antes de VET-01 y
-- no tienen fila en cliente). Los veterinarios nuevos ya se crean con usuario desde
-- POST /integrador/veterinarians.
--
-- Usuario: nombre.apellido en minúsculas y sin tildes (por ejemplo laura.gomez)
-- Contraseña: Veterinario123 (cifrada con BCrypt; cambiarla si la base no es solo de pruebas)
--
-- Ejecutar después de VET-01. Es seguro ejecutarlo de nuevo: solo toca veterinarios sin usuario
-- y se salta los nombres de usuario que ya existen. Los mensajes NOTICE dicen qué hizo.
-- =====================================================================================

-- crypt() y gen_salt('bf') generan hashes BCrypt ($2a$) que el backend reconoce
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    v_rol     INTEGER;
    v_cliente INTEGER;
    v_user    TEXT;
    r         RECORD;
BEGIN
    SELECT rol_id INTO v_rol FROM rol WHERE UPPER(descripcion) LIKE '%VETERIN%' ORDER BY rol_id LIMIT 1;
    IF v_rol IS NULL THEN
        RAISE EXCEPTION 'No existe un rol de veterinario. Crear uno: INSERT INTO rol (descripcion) VALUES (''Veterinario'');';
    END IF;

    FOR r IN
        SELECT DISTINCT p.personal_id, p.nombre, p.apellido
        FROM personal p
        JOIN medico m ON m.identificacion_medico = p.personal_id
        WHERE p.cliente_id IS NULL
        ORDER BY p.personal_id
    LOOP
        v_user := LOWER(REGEXP_REPLACE(
                TRANSLATE(r.nombre || '.' || r.apellido, 'áéíóúüñÁÉÍÓÚÜÑ', 'aeiouunAEIOUUN'), '\s+', '', 'g'));

        IF EXISTS (SELECT 1 FROM cliente WHERE username = v_user) THEN
            RAISE NOTICE 'Se omite % %: el usuario % ya existe', r.nombre, r.apellido, v_user;
            CONTINUE;
        END IF;

        INSERT INTO cliente (nombre, apellido, email, username, rol_id)
        VALUES (r.nombre, r.apellido, v_user || '@veterinaria.local', v_user, v_rol)
        RETURNING cliente_id INTO v_cliente;

        INSERT INTO user_credentials (usuario_id, password_hash)
        VALUES (v_cliente, crypt('Veterinario123', gen_salt('bf', 10)));

        UPDATE personal SET cliente_id = v_cliente WHERE personal_id = r.personal_id;

        RAISE NOTICE 'Usuario % creado para % % (contraseña Veterinario123)', v_user, r.nombre, r.apellido;
    END LOOP;
END $$;

-- Verificación: cada veterinario con su usuario
SELECT m.medico_id, p.nombre, p.apellido, c.username, COALESCE(p.activo, TRUE) AS activo
FROM medico m
JOIN personal p ON p.personal_id = m.identificacion_medico
LEFT JOIN cliente c ON c.cliente_id = p.cliente_id
ORDER BY m.medico_id;
