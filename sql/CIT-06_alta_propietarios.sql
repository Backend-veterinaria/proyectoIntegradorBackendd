-- =====================================================================================
-- CIT-06: registro de propietarios desde la clínica (RF-03) con contraseña temporal
-- Ejecutar UNA VEZ. Es seguro ejecutarlo de nuevo: usa IF NOT EXISTS.
-- =====================================================================================

-- TRUE cuando el usuario tiene una contraseña temporal entregada por el personal (al crearlo o al
-- restablecerla): el login lo indica con cambiarPassword=true y el frontend le pide elegir una nueva.
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS cambiar_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Verificación
SELECT COUNT(*) AS usuarios, COUNT(*) FILTER (WHERE cambiar_password) AS con_password_temporal FROM cliente;
