-- ============================================================================
-- Rate Limiting Configuration for /api/mail/send
-- ============================================================================

-- Rate limiting por email
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS', 
    NOW(), 
    'Máximo número de solicitudes de envío de email de reset password permitidas por email en la ventana de tiempo', 
    true, 
    '10'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'MAIL_SEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por email del endpoint de envío de mail', 
    true, 
    '60'
);

-- Rate limiting por IP
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'MAIL_SEND_RATE_LIMIT_IP_MAX_REQUESTS', 
    NOW(), 
    'Máximo número de solicitudes de envío de email de reset password permitidas por IP en la ventana de tiempo', 
    true, 
    '10'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'MAIL_SEND_RATE_LIMIT_IP_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por IP del endpoint de envío de mail', 
    true, 
    '60'
);

-- ============================================================================
-- Rate Limiting Configuration for /api/token/client
-- ============================================================================

-- Rate limiting por email
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_CLIENT_RATE_LIMIT_EMAIL_MAX_REQUESTS', 
    NOW(), 
    'Máximo número de solicitudes de generación de token de cliente permitidas por email en la ventana de tiempo', 
    true, 
    '5'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_CLIENT_RATE_LIMIT_EMAIL_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por email del endpoint de generación de token de cliente', 
    true, 
    '60'
);

-- Rate limiting por IP
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_CLIENT_RATE_LIMIT_IP_MAX_REQUESTS', 
    NOW(), 
    'Máximo número de solicitudes de generación de token de cliente permitidas por IP en la ventana de tiempo', 
    true, 
    '20'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_CLIENT_RATE_LIMIT_IP_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por IP del endpoint de generación de token de cliente', 
    true, 
    '60'
);

-- ============================================================================
-- Rate Limiting Configuration for /api/token/validate-client/generate
-- ============================================================================

-- Rate limiting por IP
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_VALIDATE_RATE_LIMIT_IP_MAX_ATTEMPTS', 
    NOW(), 
    'Máximo número de intentos de validación de token permitidos por IP en la ventana de tiempo', 
    true, 
    '10'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_VALIDATE_RATE_LIMIT_IP_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por IP del endpoint de validación de token', 
    true, 
    '15'
);

-- Rate limiting por cliente
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_VALIDATE_RATE_LIMIT_CLIENT_MAX_ATTEMPTS', 
    NOW(), 
    'Máximo número de intentos de validación de token permitidos por cliente en la ventana de tiempo', 
    true, 
    '20'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_VALIDATE_RATE_LIMIT_CLIENT_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por cliente del endpoint de validación de token', 
    true, 
    '60'
);

-- ============================================================================
-- Rate Limiting Configuration for /api/token/resend-client
-- ============================================================================

-- Rate limiting por email
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_RESEND_RATE_LIMIT_EMAIL_MAX_REQUESTS', 
    NOW(), 
    'Máximo número de reenvíos de token de cliente permitidos por email en la ventana de tiempo', 
    true, 
    '3'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_RESEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por email del endpoint de reenvío de token', 
    true, 
    '60'
);

-- Rate limiting por IP
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_RESEND_RATE_LIMIT_IP_MAX_REQUESTS', 
    NOW(), 
    'Máximo número de reenvíos de token de cliente permitidos por IP en la ventana de tiempo', 
    true, 
    '10'
);

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'TOKEN_RESEND_RATE_LIMIT_IP_WINDOW_MINUTES', 
    NOW(), 
    'Ventana de tiempo en minutos para el rate limiting por IP del endpoint de reenvío de token', 
    true, 
    '60'
);

