-- Agregar campo last_activity para rastrear última actividad de la sesión
ALTER TABLE user_invoice_point_session 
ADD COLUMN last_activity TIMESTAMP;

-- Actualizar registros existentes activos con la fecha actual
UPDATE user_invoice_point_session 
SET last_activity = start_time 
WHERE active = true AND last_activity IS NULL;

-- Actualizar registros inactivos con el end_time
UPDATE user_invoice_point_session 
SET last_activity = COALESCE(end_time, start_time) 
WHERE active = false AND last_activity IS NULL;

-- Crear índice para mejorar performance en las búsquedas de sesiones inactivas
CREATE INDEX idx_session_active_last_activity 
ON user_invoice_point_session(active, last_activity);

-- Comentario en la tabla
COMMENT ON COLUMN user_invoice_point_session.last_activity IS 
'Última actividad registrada de la sesión. Se actualiza con cada heartbeat del cliente.';

