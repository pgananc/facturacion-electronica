-- Eliminar parámetros antiguos de rate limiting que ya no se utilizan
-- Estos parámetros fueron reemplazados por el nuevo formato específico por endpoint

-- Eliminar parámetros antiguos de QUERY (reemplazados por INVOICE_QUERY_RATE_LIMIT_* y DOCUMENTS_QUERY_RATE_LIMIT_*)
DELETE FROM params WHERE code_param = 'RATE_LIMIT_QUERY_REQ_PER_MIN_TOKEN';
DELETE FROM params WHERE code_param = 'RATE_LIMIT_QUERY_REQ_PER_HOUR_IP';

-- Eliminar parámetros antiguos de DOWNLOAD (reemplazados por INVOICE_DOWNLOAD_RATE_LIMIT_*)
DELETE FROM params WHERE code_param = 'RATE_LIMIT_DOWNLOAD_PER_HOUR_TOKEN';
DELETE FROM params WHERE code_param = 'RATE_LIMIT_DOWNLOAD_PER_HOUR_IP';

-- NOTA: RATE_LIMIT_ENABLED se mantiene porque se usa globalmente por ambos patrones

