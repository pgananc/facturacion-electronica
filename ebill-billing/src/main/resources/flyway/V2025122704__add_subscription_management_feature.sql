-- =====================================================
-- FEATURE: SUBSCRIPTION_MANAGEMENT
-- Gestión de Suscripciones - Solo para SUPER_ADMIN
-- =====================================================
-- Agregar columnas adicionales a company_subscription para cancelación
ALTER TABLE company_subscription
ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(50);
-- Comentarios en las nuevas columnas
COMMENT ON COLUMN company_subscription.cancelled_at IS 'Fecha y hora de cancelación de la suscripción';
COMMENT ON COLUMN company_subscription.cancel_reason IS 'Razón de cancelación: PAYMENT_FAILURE, USER_REQUEST, VIOLATION, OTHER';
-- Índice para búsquedas por fecha de cancelación
CREATE INDEX IF NOT EXISTS idx_company_subscription_cancelled ON company_subscription (cancelled_at)
WHERE cancelled_at IS NOT NULL;
-- Índice para búsquedas por plan
CREATE INDEX IF NOT EXISTS idx_company_subscription_plan_status ON company_subscription (id_plan, subscription_status);