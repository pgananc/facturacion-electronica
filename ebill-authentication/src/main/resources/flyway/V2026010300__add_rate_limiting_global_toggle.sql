-- ============================================================================
-- Rate Limiting Global Toggle
-- ============================================================================
-- Este parámetro permite habilitar o deshabilitar el rate limiting globalmente
-- en toda la aplicación sin necesidad de redeploy.
-- 
-- Valores posibles:
--   - 'true': Rate limiting habilitado (comportamiento normal)
--   - 'false': Rate limiting deshabilitado (permite todas las solicitudes)
-- 
-- Por defecto: 'true' (habilitado por seguridad)
-- ============================================================================

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'RATE_LIMITING_ENABLED', 
    NOW(), 
    'Habilita o deshabilita el rate limiting globalmente en toda la aplicación. Valores: true (habilitado) o false (deshabilitado). Por defecto está habilitado.', 
    true, 
    'true'  -- Por defecto habilitado por seguridad
);

