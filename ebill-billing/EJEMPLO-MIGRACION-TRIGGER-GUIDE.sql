-- =====================================================
-- Migración: Activar trigger de suscripciones para guide
-- Fecha: 2025-XX-XX
-- Descripción: Crea el trigger para incrementar contadores
--              de suscripción cuando se autoriza una guía de remisión
-- =====================================================
-- Trigger para guías de remisión (solo si tiene authorization_sri)
-- La función increment_document_usage() ya tiene la lógica para 'guide'
-- Solo necesitamos crear el trigger siguiendo el mismo patrón que invoice y credit_note
DROP TRIGGER IF EXISTS trigger_guide_usage ON guide;
CREATE TRIGGER trigger_guide_usage
AFTER
INSERT ON guide FOR EACH ROW
    WHEN (NEW.id_authorization_sri IS NOT NULL) EXECUTE FUNCTION increment_document_usage();
COMMENT ON TRIGGER trigger_guide_usage ON guide IS 'Incrementa contadores de suscripción cuando se autoriza una guía de remisión';


