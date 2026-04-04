-- Parámetros para rate limiting de DOCUMENTS_QUERY
-- Endpoint: /api/invoice/documents/query

-- Límite por TOKEN (por minuto)
SELECT upsert_param(
    'DOCUMENTS_QUERY_RATE_LIMIT_TOKEN_MAX_REQUESTS',
    '100',
    'Límite máximo de solicitudes por token para consulta de documentos (/api/invoice/documents/query)'
);

SELECT upsert_param(
    'DOCUMENTS_QUERY_RATE_LIMIT_TOKEN_WINDOW_MINUTES',
    '1',
    'Ventana de tiempo en minutos para límite por token (1 = 1 minuto)'
);

-- Límite por IP (por hora)
SELECT upsert_param(
    'DOCUMENTS_QUERY_RATE_LIMIT_IP_MAX_REQUESTS',
    '500',
    'Límite máximo de solicitudes por IP para consulta de documentos (/api/invoice/documents/query)'
);

SELECT upsert_param(
    'DOCUMENTS_QUERY_RATE_LIMIT_IP_WINDOW_MINUTES',
    '60',
    'Ventana de tiempo en minutos para límite por IP (60 = 1 hora)'
);

