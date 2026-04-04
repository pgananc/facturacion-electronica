-- Agregar parámetro para la expresión cron de la tarea programada
INSERT INTO public.params (
        id_param,
        code_param,
        value_param,
        description_param,
        status,
        creation_date
    )
SELECT 5,
    'CRON_TASK_EXPRESSION',
    '0 * * * * *',
    'Expresión cron para la ejecución de la tarea programada (formato: segundo minuto hora día mes día-semana)',
    TRUE,
    NOW()
WHERE NOT EXISTS (
        SELECT 1
        FROM public.params
        WHERE code_param = 'CRON_TASK_EXPRESSION'
    );
-- Actualizar el valor si ya existe
UPDATE public.params
SET value_param = '0 */2 * * * *',
    description_param = 'Expresión cron para la ejecución de la tarea programada (formato: segundo minuto hora día mes día-semana)'
WHERE code_param = 'CRON_TASK_EXPRESSION';
-- Agregar parámetro para habilitar/deshabilitar la tarea cron
INSERT INTO public.params (
        id_param,
        code_param,
        value_param,
        description_param,
        status,
        creation_date
    )
SELECT 6,
    'CRON_TASK_ENABLED',
    'false',
    'Indica si la tarea cron está habilitada (true) o deshabilitada (false)',
    TRUE,
    NOW()
WHERE NOT EXISTS (
        SELECT 1
        FROM public.params
        WHERE code_param = 'CRON_TASK_ENABLED'
    );
-- Actualizar el valor si ya existe
UPDATE public.params
SET value_param = 'false',
    description_param = 'Indica si la tarea cron está habilitada (true) o deshabilitada (false)'
WHERE code_param = 'CRON_TASK_ENABLED';


