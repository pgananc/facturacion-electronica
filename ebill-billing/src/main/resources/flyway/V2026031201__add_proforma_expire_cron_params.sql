-- =====================================================
-- FEATURE: PROFORMA_EXPIRATION_SCHEDULER
-- Parámetros para tarea programada de expiración de proformas
-- =====================================================
-- Usa la función helper upsert_param (V2025123000__add_upsert_param_helper_function.sql)
-- para insertar si no existe o actualizar si ya existe
-- =====================================================

-- Expresión cron: 00:00:00 todos los días por defecto
SELECT upsert_param(
        'PROFORMA_EXPIRE_CRON_EXPRESSION',
        '0 0 0 * * *',
        'Expresión cron para expiración de proformas vencidas (formato: segundo minuto hora día mes día-semana). Por defecto: 00:00:00 diario'
    );

-- Habilitar/deshabilitar la tarea de expiración
SELECT upsert_param(
        'PROFORMA_EXPIRE_CRON_ENABLED',
        'true',
        'Indica si la tarea cron de expiración de proformas está habilitada (true) o deshabilitada (false)'
    );
