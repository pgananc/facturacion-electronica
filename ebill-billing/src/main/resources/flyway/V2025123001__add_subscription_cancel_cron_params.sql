-- =====================================================
-- FEATURE: SUBSCRIPTION_SCHEDULED_CANCELLATION
-- Parámetros para tarea programada de cancelaciones de suscripciones
-- =====================================================
-- NOTA: Usa funciones helper para inserción/actualización segura de datos
-- La función helper upsert_param se crea en V2025123000__add_upsert_param_helper_function.sql
-- Agregar o actualizar parámetro para la expresión cron de cancelaciones de suscripciones
-- Usa la función helper upsert_param que inserta si no existe o actualiza si ya existe
SELECT upsert_param(
        'SUBSCRIPTION_CANCEL_CRON_EXPRESSION',
        '0 0 1 * * *',
        'Expresión cron para procesar cancelaciones programadas de suscripciones (formato: segundo minuto hora día mes día-semana). Por defecto: 1:00 AM diario'
    );
-- Agregar o actualizar parámetro para habilitar/deshabilitar la tarea cron de cancelaciones
-- Usa la función helper upsert_param que inserta si no existe o actualiza si ya existe
SELECT upsert_param(
        'SUBSCRIPTION_CANCEL_CRON_ENABLED',
        'true',
        'Indica si la tarea cron de cancelaciones programadas de suscripciones está habilitada (true) o deshabilitada (false)'
    );