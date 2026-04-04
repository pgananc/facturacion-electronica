-- Parámetros para rate limiting de INVOICE_QUERY
-- Endpoint: /api/invoice/query

-- Límite por TOKEN (por minuto)
SELECT upsert_param(
    'INVOICE_QUERY_RATE_LIMIT_TOKEN_MAX_REQUESTS',
    '100',
    'Límite máximo de solicitudes por token para consulta de facturas (/api/invoice/query)'
);

SELECT upsert_param(
    'INVOICE_QUERY_RATE_LIMIT_TOKEN_WINDOW_MINUTES',
    '1',
    'Ventana de tiempo en minutos para límite por token (1 = 1 minuto)'
);

-- Límite por IP (por hora)
SELECT upsert_param(
    'INVOICE_QUERY_RATE_LIMIT_IP_MAX_REQUESTS',
    '500',
    'Límite máximo de solicitudes por IP para consulta de facturas (/api/invoice/query)'
);

SELECT upsert_param(
    'INVOICE_QUERY_RATE_LIMIT_IP_WINDOW_MINUTES',
    '60',
    'Ventana de tiempo en minutos para límite por IP (60 = 1 hora)'
);

-- Parámetros para rate limiting de INVOICE_DOWNLOAD
-- Endpoints: /api/invoice/{id}/pdf y /api/invoice/{id}/xml

-- Límite por TOKEN (por hora)
SELECT upsert_param(
    'INVOICE_DOWNLOAD_RATE_LIMIT_TOKEN_MAX_REQUESTS',
    '50',
    'Límite máximo de descargas por token para PDF/XML (/api/invoice/{id}/pdf y /api/invoice/{id}/xml)'
);

SELECT upsert_param(
    'INVOICE_DOWNLOAD_RATE_LIMIT_TOKEN_WINDOW_MINUTES',
    '60',
    'Ventana de tiempo en minutos para límite por token (60 = 1 hora)'
);

-- Límite por IP (por hora)
SELECT upsert_param(
    'INVOICE_DOWNLOAD_RATE_LIMIT_IP_MAX_REQUESTS',
    '200',
    'Límite máximo de descargas por IP para PDF/XML (/api/invoice/{id}/pdf y /api/invoice/{id}/xml)'
);

SELECT upsert_param(
    'INVOICE_DOWNLOAD_RATE_LIMIT_IP_WINDOW_MINUTES',
    '60',
    'Ventana de tiempo en minutos para límite por IP (60 = 1 hora)'
);

