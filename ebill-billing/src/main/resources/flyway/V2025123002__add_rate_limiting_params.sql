-- =====================================================
-- RATE LIMITING PARAMETERS
-- Version: 1.0
-- Date: 2025-01-15
-- =====================================================
-- Insertar parámetros de rate limiting usando la función upsert_param
SELECT upsert_param(
    'RATE_LIMIT_QUERY_REQ_PER_MIN_TOKEN',
    '100',
    'Límite de requests por minuto por token para consulta de documentos (/api/invoice/documents/query)'
);

SELECT upsert_param(
    'RATE_LIMIT_QUERY_REQ_PER_HOUR_IP',
    '500',
    'Límite de requests por hora por IP para consulta de documentos (/api/invoice/documents/query)'
);

SELECT upsert_param(
    'RATE_LIMIT_DOWNLOAD_PER_HOUR_TOKEN',
    '50',
    'Límite de descargas por hora por token para PDF/XML (/api/invoice/{id}/pdf y /api/invoice/{id}/xml)'
);

SELECT upsert_param(
    'RATE_LIMIT_DOWNLOAD_PER_HOUR_IP',
    '200',
    'Límite de descargas por hora por IP para PDF/XML (/api/invoice/{id}/pdf y /api/invoice/{id}/xml)'
);

SELECT upsert_param(
    'RATE_LIMIT_ENABLED',
    'true',
    'Habilitar/deshabilitar rate limiting globalmente'
);