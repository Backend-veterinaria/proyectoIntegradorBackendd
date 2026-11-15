-- =====================================================================================
-- VET-01: gestión de veterinarios
-- Ejecutar UNA VEZ en la base de datos antes de usar los endpoints /integrador/veterinarians.
-- Es seguro ejecutarlo de nuevo: usa IF NOT EXISTS.
-- =====================================================================================

BEGIN;

-- 1. Estado del personal. Inactivar no borra el registro: el veterinario conserva su historial
--    de citas, pero deja de ofrecerse para nuevas citas y no puede iniciar sesión (CPA-09).
ALTER TABLE personal ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Vínculo entre el personal y su usuario para iniciar sesión (los usuarios están en la
--    tabla cliente). Lo usa también VET-02 para que cada veterinario vea su propia agenda.
ALTER TABLE personal ADD COLUMN IF NOT EXISTS cliente_id INTEGER REFERENCES cliente (cliente_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_personal_cliente_id ON personal (cliente_id);

COMMIT;

-- =====================================================================================
-- Verificaciones (solo lectura)
-- =====================================================================================

-- a) Debe existir un rol cuya descripción contenga "VETERIN"; si no, crear un veterinario falla.
--    Si no existe: INSERT INTO rol (descripcion) VALUES ('Veterinario');
SELECT rol_id, descripcion FROM rol WHERE UPPER(descripcion) LIKE '%VETERIN%';

-- b) personal_id y medico_id deben generarse automáticamente (serial o identity): column_default
--    debe tener un nextval(...) o is_identity = 'YES'.
SELECT table_name, column_name, column_default, is_identity
FROM information_schema.columns
WHERE table_name IN ('personal', 'medico') AND column_name IN ('personal_id', 'medico_id');
