-- =====================================================
-- FIX: Eliminar migración V2025121402 del historial
-- para permitir que se vuelva a aplicar
-- =====================================================
-- Ver el estado actual
SELECT version,
    checksum,
    description,
    installed_on,
    success
FROM flyway_schema_history
WHERE version = '2025121402';
-- IMPORTANTE: Solo ejecutar si las tablas YA TIENEN la columna status
-- Verificar primero:
-- SELECT column_name FROM information_schema.columns 
-- WHERE table_name = 'subscription_plan' AND column_name = 'status';
-- Eliminar el registro de la migración
DELETE FROM flyway_schema_history
WHERE version = '2025121402';
-- Verificar que se eliminó
SELECT version,
    checksum,
    description,
    installed_on,
    success
FROM flyway_schema_history
WHERE version = '2025121402';
-- =====================================================
-- Resultado esperado: 0 rows
-- Después de esto, reiniciar la aplicación
-- Flyway volverá a aplicar V2025121402 con el nuevo checksum
-- =====================================================