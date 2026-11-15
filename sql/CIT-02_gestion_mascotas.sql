-- =====================================================================================
-- CIT-02: gestión de mascotas (edición e inactivación)
-- Ejecutar UNA VEZ en la base de datos. Es seguro ejecutarlo de nuevo: usa IF NOT EXISTS.
-- =====================================================================================

-- Inactivar no borra la mascota: conserva su historial de citas, pero deja de ofrecerse
-- para nuevas citas y no se le pueden agendar (CPA-08).
ALTER TABLE mascota ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;

-- Verificación
SELECT COUNT(*) AS total, COUNT(*) FILTER (WHERE activo) AS activas FROM mascota;
