-- ===================================================================
-- Script: Asignar Suscripción ENTERPRISE a WALLPARISOFT
-- Descripción: Crea una suscripción ENTERPRISE para la compañía WALLPARISOFT
--              solo si la compañía no tiene una suscripción activa
-- Fecha: 2026-01-13
-- ===================================================================
DO $$
DECLARE v_company_id BIGINT := 1;
-- ID de la compañía WALLPARISOFT
v_plan_id BIGINT;
v_subscription_exists BOOLEAN;
v_start_date TIMESTAMP;
v_expires_at TIMESTAMP;
v_subscription_id BIGINT;
BEGIN -- =================================================================
-- PASO 1: Verificar si la compañía ya tiene una suscripción activa
-- =================================================================
SELECT EXISTS(
        SELECT 1
        FROM public.company_subscription
        WHERE id_company = v_company_id
            AND subscription_status = 'ACTIVE'
            AND status = true -- Soft delete check
            AND (
                expires_at IS NULL
                OR expires_at > CURRENT_TIMESTAMP
            )
    ) INTO v_subscription_exists;
IF v_subscription_exists THEN RAISE NOTICE '⚠️ La compañía (ID: %) ya tiene una suscripción activa. No se creará una nueva suscripción.',
v_company_id;
RETURN;
END IF;
RAISE NOTICE '✅ La compañía no tiene suscripción activa. Procediendo a crear suscripción ENTERPRISE...';
-- =================================================================
-- PASO 2: Buscar el plan ENTERPRISE
-- =================================================================
SELECT id_plan INTO v_plan_id
FROM public.subscription_plan
WHERE UPPER(TRIM(code)) = 'ENTERPRISE'
    AND status = true
LIMIT 1;
-- Verificar si se encontró el plan
IF v_plan_id IS NULL THEN RAISE EXCEPTION 'No se encontró el plan ENTERPRISE en la base de datos. Asegúrate de que el plan esté creado y activo.';
END IF;
RAISE NOTICE '✅ Plan ENTERPRISE encontrado: ID = %',
v_plan_id;
-- =================================================================
-- PASO 3: Calcular fechas (inicio hoy, expiración en 1 año)
-- =================================================================
v_start_date := CURRENT_TIMESTAMP;
v_expires_at := CURRENT_TIMESTAMP + INTERVAL '1 year';
RAISE NOTICE '📅 Fecha de inicio: %',
v_start_date;
RAISE NOTICE '📅 Fecha de expiración: %',
v_expires_at;
-- =================================================================
-- PASO 4: Insertar la suscripción
-- =================================================================
INSERT INTO public.company_subscription (
        id_company,
        id_plan,
        subscription_status,
        start_date,
        expires_at,
        current_year_usage,
        last_reset_date,
        payment_method,
        notes,
        creation_date,
        update_date,
        status
    )
VALUES (
        v_company_id,
        v_plan_id,
        'ACTIVE',
        v_start_date,
        v_expires_at,
        0,
        -- Uso inicial en 0
        v_start_date,
        -- Fecha de reset igual a la fecha de inicio
        'MANUAL',
        -- Método de pago manual por defecto
        'Suscripción ENTERPRISE asignada automáticamente mediante script',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        true -- status de BaseEntity (soft delete)
    )
RETURNING id_subscription INTO v_subscription_id;
RAISE NOTICE '✅ Suscripción creada exitosamente: ID = %',
v_subscription_id;
RAISE NOTICE '✅ Compañía ID: %',
v_company_id;
RAISE NOTICE '✅ Plan: ENTERPRISE (ID: %)',
v_plan_id;
RAISE NOTICE '✅ Estado: ACTIVE';
RAISE NOTICE '✅ Vigencia: % a %',
v_start_date,
v_expires_at;
EXCEPTION
WHEN OTHERS THEN RAISE EXCEPTION 'Error al crear la suscripción: %',
SQLERRM;
END $$;