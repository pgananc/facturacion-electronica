-- =====================================================
-- SUBSCRIPTION MANAGEMENT SYSTEM
-- Version: 1.0
-- Date: 2025-12-14
-- =====================================================
-- =====================================================
-- TABLE: subscription_plan
-- Catálogo de planes de suscripción disponibles
-- =====================================================
CREATE TABLE IF NOT EXISTS subscription_plan (
    id_plan BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price_annual NUMERIC(10, 2) NOT NULL DEFAULT 0,
    price_monthly NUMERIC(10, 2) NOT NULL DEFAULT 0,
    document_limit_yearly INTEGER NOT NULL DEFAULT 0,
    user_limit INTEGER NOT NULL DEFAULT 1,
    establishment_limit INTEGER NOT NULL DEFAULT 1,
    issuance_point_limit INTEGER NOT NULL DEFAULT 1,
    -- Features (características del plan)
    has_api_access BOOLEAN NOT NULL DEFAULT FALSE,
    has_multi_user BOOLEAN NOT NULL DEFAULT FALSE,
    has_advanced_reports BOOLEAN NOT NULL DEFAULT FALSE,
    has_electronic_invoice BOOLEAN NOT NULL DEFAULT TRUE,
    has_credit_note BOOLEAN NOT NULL DEFAULT TRUE,
    has_debit_note BOOLEAN NOT NULL DEFAULT FALSE,
    has_retention BOOLEAN NOT NULL DEFAULT FALSE,
    has_guide BOOLEAN NOT NULL DEFAULT FALSE,
    has_priority_support BOOLEAN NOT NULL DEFAULT FALSE,
    has_custom_branding BOOLEAN NOT NULL DEFAULT FALSE,
    has_automated_backups BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP
);
-- Índices para subscription_plan
CREATE INDEX IF NOT EXISTS idx_subscription_plan_code ON subscription_plan (code);
CREATE INDEX IF NOT EXISTS idx_subscription_plan_active ON subscription_plan (is_active);
-- =====================================================
-- TABLE: company_subscription
-- Suscripciones activas de las compañías
-- =====================================================
CREATE TABLE IF NOT EXISTS company_subscription (
    id_subscription BIGSERIAL PRIMARY KEY,
    id_company BIGINT NOT NULL,
    id_plan BIGINT NOT NULL,
    subscription_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    current_year_usage INTEGER NOT NULL DEFAULT 0,
    last_reset_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Payment information
    payment_method VARCHAR(50),
    last_payment_date TIMESTAMP,
    last_payment_amount NUMERIC(10, 2),
    payment_reference VARCHAR(255),
    notes TEXT,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_company_subscription_plan FOREIGN KEY (id_plan) REFERENCES subscription_plan (id_plan),
    CONSTRAINT chk_subscription_status CHECK (
        subscription_status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED')
    )
);
-- Índices para company_subscription
CREATE INDEX IF NOT EXISTS idx_company_subscription_company ON company_subscription (id_company);
CREATE INDEX IF NOT EXISTS idx_company_subscription_plan ON company_subscription (id_plan);
CREATE INDEX IF NOT EXISTS idx_company_subscription_status ON company_subscription (subscription_status);
CREATE INDEX IF NOT EXISTS idx_company_subscription_expires ON company_subscription (expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_company_subscription_active ON company_subscription (id_company, subscription_status)
WHERE subscription_status = 'ACTIVE';
-- =====================================================
-- TABLE: subscription_usage_history
-- Historial de uso mensual por compañía
-- =====================================================
CREATE TABLE IF NOT EXISTS subscription_usage_history (
    id_history BIGSERIAL PRIMARY KEY,
    id_company BIGINT NOT NULL,
    id_subscription BIGINT NOT NULL,
    month_year VARCHAR(7) NOT NULL,
    -- Format: YYYY-MM
    invoices_count INTEGER NOT NULL DEFAULT 0,
    credit_notes_count INTEGER NOT NULL DEFAULT 0,
    debit_notes_count INTEGER NOT NULL DEFAULT 0,
    retentions_count INTEGER NOT NULL DEFAULT 0,
    guides_count INTEGER NOT NULL DEFAULT 0,
    total_documents INTEGER NOT NULL DEFAULT 0,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP,
    CONSTRAINT fk_subscription_usage_subscription FOREIGN KEY (id_subscription) REFERENCES company_subscription (id_subscription),
    CONSTRAINT uq_usage_company_month UNIQUE (id_company, month_year)
);
-- Índices para subscription_usage_history
CREATE INDEX IF NOT EXISTS idx_usage_history_company ON subscription_usage_history (id_company);
CREATE INDEX IF NOT EXISTS idx_usage_history_subscription ON subscription_usage_history (id_subscription);
CREATE INDEX IF NOT EXISTS idx_usage_history_month ON subscription_usage_history (month_year);
-- =====================================================
-- SECUENCIAS
-- =====================================================
CREATE SEQUENCE IF NOT EXISTS subscription_plan_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS company_subscription_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS subscription_usage_history_seq START WITH 1 INCREMENT BY 1;
-- =====================================================
-- FUNCIÓN: increment_document_usage
-- Incrementa el contador de uso cuando se autoriza un documento
-- =====================================================
CREATE OR REPLACE FUNCTION increment_document_usage() RETURNS TRIGGER AS $$
DECLARE v_id_company BIGINT;
v_id_subscription BIGINT;
v_month_year VARCHAR(7);
v_document_type VARCHAR(20);
BEGIN -- Determinar el tipo de documento y el id_company
IF TG_TABLE_NAME = 'invoice' THEN v_id_company := NEW.id_company;
v_document_type := 'INVOICE';
ELSIF TG_TABLE_NAME = 'credit_note' THEN v_id_company := NEW.id_company;
v_document_type := 'CREDIT_NOTE';
ELSIF TG_TABLE_NAME = 'debit_note' THEN v_id_company := NEW.id_company;
v_document_type := 'DEBIT_NOTE';
ELSIF TG_TABLE_NAME = 'retention' THEN v_id_company := NEW.id_company;
v_document_type := 'RETENTION';
ELSIF TG_TABLE_NAME = 'guide' THEN v_id_company := NEW.id_company;
v_document_type := 'GUIDE';
ELSE RETURN NEW;
END IF;
-- Obtener suscripción activa
SELECT id_subscription INTO v_id_subscription
FROM company_subscription
WHERE id_company = v_id_company
    AND status = 'ACTIVE'
LIMIT 1;
-- Si no hay suscripción activa, no hacer nada
IF v_id_subscription IS NULL THEN RETURN NEW;
END IF;
-- Obtener mes/año actual
v_month_year := TO_CHAR(CURRENT_DATE, 'YYYY-MM');
-- Insertar o actualizar historial mensual
INSERT INTO subscription_usage_history (
        id_company,
        id_subscription,
        month_year,
        invoices_count,
        credit_notes_count,
        debit_notes_count,
        retentions_count,
        guides_count,
        total_documents
    )
VALUES (
        v_id_company,
        v_id_subscription,
        v_month_year,
        CASE
            WHEN v_document_type = 'INVOICE' THEN 1
            ELSE 0
        END,
        CASE
            WHEN v_document_type = 'CREDIT_NOTE' THEN 1
            ELSE 0
        END,
        CASE
            WHEN v_document_type = 'DEBIT_NOTE' THEN 1
            ELSE 0
        END,
        CASE
            WHEN v_document_type = 'RETENTION' THEN 1
            ELSE 0
        END,
        CASE
            WHEN v_document_type = 'GUIDE' THEN 1
            ELSE 0
        END,
        1
    ) ON CONFLICT (id_company, month_year) DO
UPDATE
SET invoices_count = subscription_usage_history.invoices_count + CASE
        WHEN v_document_type = 'INVOICE' THEN 1
        ELSE 0
    END,
    credit_notes_count = subscription_usage_history.credit_notes_count + CASE
        WHEN v_document_type = 'CREDIT_NOTE' THEN 1
        ELSE 0
    END,
    debit_notes_count = subscription_usage_history.debit_notes_count + CASE
        WHEN v_document_type = 'DEBIT_NOTE' THEN 1
        ELSE 0
    END,
    retentions_count = subscription_usage_history.retentions_count + CASE
        WHEN v_document_type = 'RETENTION' THEN 1
        ELSE 0
    END,
    guides_count = subscription_usage_history.guides_count + CASE
        WHEN v_document_type = 'GUIDE' THEN 1
        ELSE 0
    END,
    total_documents = subscription_usage_history.total_documents + 1,
    update_date = CURRENT_TIMESTAMP;
-- Actualizar contador anual en company_subscription
UPDATE company_subscription
SET current_year_usage = current_year_usage + 1,
    update_date = CURRENT_TIMESTAMP
WHERE id_subscription = v_id_subscription;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- =====================================================
-- TRIGGERS: Incrementar uso cuando se autoriza documento
-- =====================================================
-- Trigger para facturas (solo si tiene authorization_sri)
DROP TRIGGER IF EXISTS trigger_invoice_usage ON invoice;
CREATE TRIGGER trigger_invoice_usage
AFTER
INSERT ON invoice FOR EACH ROW
    WHEN (NEW.id_authorization_sri IS NOT NULL) EXECUTE FUNCTION increment_document_usage();
-- Trigger para notas de crédito (solo si tiene authorization_sri)
DROP TRIGGER IF EXISTS trigger_credit_note_usage ON credit_note;
CREATE TRIGGER trigger_credit_note_usage
AFTER
INSERT ON credit_note FOR EACH ROW
    WHEN (NEW.id_authorization_sri IS NOT NULL) EXECUTE FUNCTION increment_document_usage();
-- Triggers para otros documentos (si existen las tablas)
-- Se crearán cuando se implementen estos módulos
-- CREATE TRIGGER trigger_debit_note_usage
-- AFTER INSERT ON debit_note
-- FOR EACH ROW
-- WHEN (NEW.id_authorization_sri IS NOT NULL)
-- EXECUTE FUNCTION increment_document_usage();
-- CREATE TRIGGER trigger_retention_usage
-- AFTER INSERT ON retention
-- FOR EACH ROW
-- WHEN (NEW.id_authorization_sri IS NOT NULL)
-- EXECUTE FUNCTION increment_document_usage();
-- CREATE TRIGGER trigger_guide_usage
-- AFTER INSERT ON guide
-- FOR EACH ROW
-- WHEN (NEW.id_authorization_sri IS NOT NULL)
-- EXECUTE FUNCTION increment_document_usage();
-- =====================================================
-- DATA: Insertar planes de suscripción usando funciones helper
-- =====================================================
-- NOTA: Las funciones helper se crean en V2025122701__create_helper_functions.sql
-- Estas funciones usan ON CONFLICT DO NOTHING para evitar errores en despliegues concurrentes
-- Plan FREE
SELECT insert_subscription_plan_if_not_exists(
        'FREE',
        'Plan Gratuito',
        'Plan básico sin costo para emprendedores',
        0,
        0,
        50,
        1,
        1,
        1,
        FALSE,
        FALSE,
        FALSE,
        TRUE,
        TRUE,
        FALSE,
        FALSE,
        FALSE,
        FALSE,
        FALSE,
        FALSE,
        TRUE,
        1
    );
-- Plan BASIC
SELECT insert_subscription_plan_if_not_exists(
        'BASIC',
        'Plan Básico',
        'Para pequeñas empresas en crecimiento',
        120,
        12,
        500,
        3,
        2,
        3,
        FALSE,
        TRUE,
        FALSE,
        TRUE,
        TRUE,
        TRUE,
        FALSE,
        FALSE,
        FALSE,
        FALSE,
        TRUE,
        TRUE,
        2
    );
-- Plan PRO
SELECT insert_subscription_plan_if_not_exists(
        'PRO',
        'Plan Profesional',
        'Para empresas establecidas con alto volumen',
        300,
        30,
        2000,
        10,
        5,
        10,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        FALSE,
        TRUE,
        FALSE,
        TRUE,
        TRUE,
        3
    );
-- Plan BUSINESS
SELECT insert_subscription_plan_if_not_exists(
        'BUSINESS',
        'Plan Empresarial',
        'Para medianas y grandes empresas',
        600,
        60,
        5000,
        25,
        10,
        20,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        4
    );
-- Plan ENTERPRISE
SELECT insert_subscription_plan_if_not_exists(
        'ENTERPRISE',
        'Plan Corporativo',
        'Solución completa sin límites',
        1200,
        120,
        999999,
        999,
        999,
        999,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        TRUE,
        5
    );
-- =====================================================
-- DATA: Migrar compañías existentes al plan FREE usando función helper
-- =====================================================
-- NOTA: La función helper ensure_company_subscription_free se crea en V2025122701__create_helper_functions.sql
-- Esta función usa NOT EXISTS para evitar duplicados en despliegues concurrentes
-- Migrar compañías que tienen facturas
SELECT ensure_company_subscription_free(i.id_company)
FROM invoice i
GROUP BY i.id_company;
-- Migrar compañías que solo tienen notas de crédito
SELECT ensure_company_subscription_free(cn.id_company)
FROM credit_note cn
GROUP BY cn.id_company;
-- =====================================================
-- COMENTARIOS FINALES
-- =====================================================
COMMENT ON TABLE subscription_plan IS 'Catálogo de planes de suscripción disponibles';
COMMENT ON TABLE company_subscription IS 'Suscripciones activas de las compañías';
COMMENT ON TABLE subscription_usage_history IS 'Historial mensual de uso de documentos por compañía';
COMMENT ON FUNCTION increment_document_usage() IS 'Incrementa contadores de uso cuando se autoriza un documento electrónico';
COMMENT ON COLUMN subscription_plan.document_limit_yearly IS 'Límite de documentos por año (0 = ilimitado en lógica de negocio)';
COMMENT ON COLUMN subscription_plan.user_limit IS 'Número máximo de usuarios permitidos';
COMMENT ON COLUMN subscription_plan.establishment_limit IS 'Número máximo de establecimientos permitidos';
COMMENT ON COLUMN subscription_plan.issuance_point_limit IS 'Número máximo de puntos de emisión permitidos';
COMMENT ON COLUMN company_subscription.current_year_usage IS 'Contador de documentos emitidos en el año actual';
COMMENT ON COLUMN company_subscription.last_reset_date IS 'Última fecha de reseteo del contador (inicio de año)';
COMMENT ON COLUMN company_subscription.subscription_status IS 'ACTIVE, EXPIRED, CANCELLED, SUSPENDED';
COMMENT ON COLUMN company_subscription.status IS 'Estado de registro (activo/inactivo)';