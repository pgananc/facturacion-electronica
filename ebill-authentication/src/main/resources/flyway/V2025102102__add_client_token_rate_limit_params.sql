
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES ((SELECT MAX(id_param) + 1 FROM public.params), 'CLIENT_TOKEN_MAX_ATTEMPTS', NOW(), 'Máximo número de intentos permitidos para solicitar token de cliente', true,
        '3');
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES ((SELECT MAX(id_param) + 1 FROM public.params), 'CLIENT_TOKEN_COOLDOWN_MINUTES', NOW(), 'Tiempo de espera en minutos después de alcanzar el máximo de intentos', true,
        '120');
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES ((SELECT MAX(id_param) + 1 FROM public.params), 'CLIENT_TOKEN_REDIS_KEY_PREFIX', NOW(), 'Prefijo para las claves de Redis en rate limiting de tokens', true,
        'client_token_attempts');
-- Comentarios
COMMENT ON COLUMN public.params.code_param IS 'Código único del parámetro';
COMMENT ON COLUMN public.params.value_param IS 'Valor del parámetro';
COMMENT ON COLUMN public.params.description_param IS 'Descripción del parámetro';
