-- Verificar que las columnas ya existan
SELECT table_name,
  column_name,
  data_type
FROM information_schema.columns
WHERE table_name IN (
    'subscription_plan',
    'subscription_usage_history'
  )
  AND column_name = 'status';
-- Ver estado actual de flyway
SELECT version,
  checksum,
  description,
  installed_on,
  success
FROM flyway_schema_history
WHERE version = '2025121402';
-- Eliminar el registro de la migración
DELETE FROM flyway_schema_history
WHERE version = '2025121402';
-- Confirmar que se eliminó
SELECT COUNT(*) as deleted
FROM flyway_schema_history
WHERE version = '2025121402';