-- =====================================================
-- Migración: Agregar campo status al INSERT de increment_document_usage
-- Fecha: 2025-12-23
-- Descripción: Agrega el campo 'status' al INSERT en subscription_usage_history
--              que es requerido por la columna NOT NULL agregada en V2025121402
-- =====================================================

-- Corregir la función increment_document_usage() para incluir el campo status
CREATE OR REPLACE FUNCTION increment_document_usage() RETURNS TRIGGER AS $$
DECLARE 
    v_id_company BIGINT;
    v_id_subscription BIGINT;
    v_month_year VARCHAR(7);
    v_document_type VARCHAR(20);
BEGIN 
    -- Determinar el tipo de documento y el id_company
    IF TG_TABLE_NAME = 'invoice' THEN 
        v_id_company := NEW.id_company;
        v_document_type := 'INVOICE';
    ELSIF TG_TABLE_NAME = 'credit_note' THEN 
        v_id_company := NEW.id_company;
        v_document_type := 'CREDIT_NOTE';
    ELSIF TG_TABLE_NAME = 'debit_note' THEN 
        v_id_company := NEW.id_company;
        v_document_type := 'DEBIT_NOTE';
    ELSIF TG_TABLE_NAME = 'retention' THEN 
        v_id_company := NEW.id_company;
        v_document_type := 'RETENTION';
    ELSIF TG_TABLE_NAME = 'guide' THEN 
        v_id_company := NEW.id_company;
        v_document_type := 'GUIDE';
    ELSE 
        RETURN NEW;
    END IF;

    -- Obtener suscripción activa
    -- CORRECCIÓN: Usar subscription_status (VARCHAR) en lugar de status (BOOLEAN)
    SELECT id_subscription INTO v_id_subscription
    FROM company_subscription
    WHERE id_company = v_id_company
        AND subscription_status = 'ACTIVE'
        AND status = TRUE
    LIMIT 1;

    -- Si no hay suscripción activa, no hacer nada
    IF v_id_subscription IS NULL THEN 
        RETURN NEW;
    END IF;

    -- Obtener mes/año actual
    v_month_year := TO_CHAR(CURRENT_DATE, 'YYYY-MM');

    -- Insertar o actualizar historial mensual
    -- CORRECCIÓN: Incluir el campo status (BOOLEAN) requerido por NOT NULL
    INSERT INTO subscription_usage_history (
        id_company,
        id_subscription,
        month_year,
        invoices_count,
        credit_notes_count,
        debit_notes_count,
        retentions_count,
        guides_count,
        total_documents,
        status
    )
    VALUES (
        v_id_company,
        v_id_subscription,
        v_month_year,
        CASE WHEN v_document_type = 'INVOICE' THEN 1 ELSE 0 END,
        CASE WHEN v_document_type = 'CREDIT_NOTE' THEN 1 ELSE 0 END,
        CASE WHEN v_document_type = 'DEBIT_NOTE' THEN 1 ELSE 0 END,
        CASE WHEN v_document_type = 'RETENTION' THEN 1 ELSE 0 END,
        CASE WHEN v_document_type = 'GUIDE' THEN 1 ELSE 0 END,
        1,
        TRUE
    ) 
    ON CONFLICT (id_company, month_year) DO UPDATE
    SET 
        invoices_count = subscription_usage_history.invoices_count + CASE WHEN v_document_type = 'INVOICE' THEN 1 ELSE 0 END,
        credit_notes_count = subscription_usage_history.credit_notes_count + CASE WHEN v_document_type = 'CREDIT_NOTE' THEN 1 ELSE 0 END,
        debit_notes_count = subscription_usage_history.debit_notes_count + CASE WHEN v_document_type = 'DEBIT_NOTE' THEN 1 ELSE 0 END,
        retentions_count = subscription_usage_history.retentions_count + CASE WHEN v_document_type = 'RETENTION' THEN 1 ELSE 0 END,
        guides_count = subscription_usage_history.guides_count + CASE WHEN v_document_type = 'GUIDE' THEN 1 ELSE 0 END,
        total_documents = subscription_usage_history.total_documents + 1,
        update_date = CURRENT_TIMESTAMP;

    -- Actualizar contador anual en company_subscription
    UPDATE company_subscription
    SET 
        current_year_usage = current_year_usage + 1,
        update_date = CURRENT_TIMESTAMP
    WHERE id_subscription = v_id_subscription;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION increment_document_usage() IS 'Incrementa contadores de uso cuando se autoriza un documento electrónico. CORREGIDO: Usa subscription_status en lugar de status y incluye el campo status en el INSERT.';

