-- =====================================================
-- HELPER FUNCTIONS FOR SAFE DATA INSERTION
-- Fecha: 2025-12-23
-- Descripción: Funciones helper para inserción segura de datos
--              que evitan conflictos en despliegues concurrentes
-- =====================================================
-- =====================================================
-- FUNCTION: insert_subscription_plan_if_not_exists
-- Inserta un plan de suscripción solo si no existe (por código)
-- =====================================================
CREATE OR REPLACE FUNCTION insert_subscription_plan_if_not_exists(
        p_code VARCHAR(50),
        p_name VARCHAR(100),
        p_description TEXT,
        p_price_annual NUMERIC(10, 2),
        p_price_monthly NUMERIC(10, 2),
        p_document_limit_yearly INTEGER,
        p_user_limit INTEGER,
        p_establishment_limit INTEGER,
        p_issuance_point_limit INTEGER,
        p_has_api_access BOOLEAN,
        p_has_multi_user BOOLEAN,
        p_has_advanced_reports BOOLEAN,
        p_has_electronic_invoice BOOLEAN,
        p_has_credit_note BOOLEAN,
        p_has_debit_note BOOLEAN,
        p_has_retention BOOLEAN,
        p_has_guide BOOLEAN,
        p_has_priority_support BOOLEAN,
        p_has_custom_branding BOOLEAN,
        p_has_automated_backups BOOLEAN,
        p_is_active BOOLEAN,
        p_display_order INTEGER
    ) RETURNS VOID AS $$ BEGIN
INSERT INTO subscription_plan (
        code,
        name,
        description,
        price_annual,
        price_monthly,
        document_limit_yearly,
        user_limit,
        establishment_limit,
        issuance_point_limit,
        has_api_access,
        has_multi_user,
        has_advanced_reports,
        has_electronic_invoice,
        has_credit_note,
        has_debit_note,
        has_retention,
        has_guide,
        has_priority_support,
        has_custom_branding,
        has_automated_backups,
        is_active,
        display_order
    )
VALUES (
        p_code,
        p_name,
        p_description,
        p_price_annual,
        p_price_monthly,
        p_document_limit_yearly,
        p_user_limit,
        p_establishment_limit,
        p_issuance_point_limit,
        p_has_api_access,
        p_has_multi_user,
        p_has_advanced_reports,
        p_has_electronic_invoice,
        p_has_credit_note,
        p_has_debit_note,
        p_has_retention,
        p_has_guide,
        p_has_priority_support,
        p_has_custom_branding,
        p_has_automated_backups,
        p_is_active,
        p_display_order
    ) ON CONFLICT (code) DO NOTHING;
END;
$$ LANGUAGE plpgsql;
COMMENT ON FUNCTION insert_subscription_plan_if_not_exists IS 'Inserta un plan de suscripción solo si no existe (por código). Evita conflictos en despliegues concurrentes.';
-- =====================================================
-- FUNCTION: insert_payment_method_if_not_exists
-- Inserta un método de pago solo si no existe (por código)
-- =====================================================
CREATE OR REPLACE FUNCTION insert_payment_method_if_not_exists(
        p_payment_method_code VARCHAR(10),
        p_payment_method_type VARCHAR(255)
    ) RETURNS VOID AS $$
DECLARE v_exists BOOLEAN;
BEGIN -- Verificar si ya existe un método de pago con este código
SELECT EXISTS(
        SELECT 1
        FROM payment_method
        WHERE payment_method_code = p_payment_method_code
    ) INTO v_exists;
-- Solo insertar si no existe
IF NOT v_exists THEN
INSERT INTO payment_method (
        payment_method_type,
        status,
        creation_date,
        update_date,
        payment_method_code
    )
VALUES (
        p_payment_method_type,
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        p_payment_method_code
    );
END IF;
END;
$$ LANGUAGE plpgsql;
COMMENT ON FUNCTION insert_payment_method_if_not_exists IS 'Inserta un método de pago solo si no existe (por código). Evita conflictos en despliegues concurrentes.';
-- =====================================================
-- FUNCTION: insert_param_if_not_exists
-- Inserta un parámetro solo si no existe (por código)
-- =====================================================
CREATE OR REPLACE FUNCTION insert_param_if_not_exists(
        p_code_param VARCHAR(255),
        p_value_param TEXT,
        p_description_param VARCHAR(255)
    ) RETURNS VOID AS $$
DECLARE v_next_id BIGINT;
BEGIN -- Obtener el siguiente ID disponible
SELECT COALESCE(MAX(id_param), 0) + 1 INTO v_next_id
FROM params;
INSERT INTO params (
        id_param,
        code_param,
        value_param,
        description_param,
        status,
        creation_date
    )
VALUES (
        v_next_id,
        p_code_param,
        p_value_param,
        p_description_param,
        TRUE,
        CURRENT_TIMESTAMP
    ) ON CONFLICT (code_param) DO NOTHING;
END;
$$ LANGUAGE plpgsql;
COMMENT ON FUNCTION insert_param_if_not_exists IS 'Inserta un parámetro solo si no existe (por código). Calcula automáticamente el siguiente ID disponible. Evita conflictos en despliegues concurrentes.';
-- =====================================================
-- FUNCTION: ensure_company_subscription_free
-- Asegura que una compañía tenga una suscripción FREE si no tiene ninguna
-- =====================================================
CREATE OR REPLACE FUNCTION ensure_company_subscription_free(p_id_company BIGINT) RETURNS VOID AS $$
DECLARE v_id_plan_free BIGINT;
BEGIN -- Obtener el ID del plan FREE
SELECT id_plan INTO v_id_plan_free
FROM subscription_plan
WHERE code = 'FREE'
LIMIT 1;
-- Si no existe el plan FREE, no hacer nada (debería existir)
IF v_id_plan_free IS NULL THEN RETURN;
END IF;
-- Insertar suscripción FREE solo si no existe ninguna suscripción para esta compañía
INSERT INTO company_subscription (
        id_company,
        id_plan,
        subscription_status,
        start_date,
        expires_at,
        current_year_usage,
        last_reset_date,
        notes,
        status
    )
SELECT p_id_company,
    v_id_plan_free,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '1 year',
    0,
    CURRENT_TIMESTAMP,
    'Migración automática a plan FREE',
    TRUE
WHERE NOT EXISTS (
        SELECT 1
        FROM company_subscription cs
        WHERE cs.id_company = p_id_company
    );
END;
$$ LANGUAGE plpgsql;
COMMENT ON FUNCTION ensure_company_subscription_free IS 'Asegura que una compañía tenga una suscripción FREE si no tiene ninguna. Útil para migraciones de datos existentes.';