-- =====================================================
-- Migración: Catálogo de motivos de NC y campos adicionales
-- Fecha: 2025-12-07
-- Descripción: Crea tabla credit_note_reason y agrega campos
--              faltantes en credit_note y credit_note_detail
-- =====================================================
-- 1. Crear secuencia para credit_note_reason
CREATE SEQUENCE IF NOT EXISTS credit_note_reason_seq START WITH 1 INCREMENT BY 1;
-- 2. Crear tabla credit_note_reason (Catálogo de Motivos según SRI Ecuador)
CREATE TABLE credit_note_reason (
    id_reason BIGINT NOT NULL PRIMARY KEY DEFAULT nextval('credit_note_reason_seq'),
    code VARCHAR(2) NOT NULL UNIQUE,
    description VARCHAR(100) NOT NULL,
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT NOW(),
    update_date TIMESTAMP
);
COMMENT ON TABLE credit_note_reason IS 'Catálogo de motivos de nota de crédito según SRI Ecuador';
COMMENT ON COLUMN credit_note_reason.code IS 'Código del motivo (01-05)';
COMMENT ON COLUMN credit_note_reason.description IS 'Descripción del motivo';
-- 3. Insertar datos iniciales (según SRI Ecuador)
INSERT INTO credit_note_reason (code, description, creation_date)
VALUES ('01', 'Devolución de bienes', NOW()),
    ('02', 'Descuento comercial posterior', NOW()),
    (
        '03',
        'Corrección de error en facturación',
        NOW()
    ),
    ('04', 'Rebaja o bonificación', NOW()),
    ('05', 'Resciliación del contrato', NOW());
-- 4. Agregar columna id_reason a credit_note (FK a credit_note_reason)
ALTER TABLE credit_note
ADD COLUMN IF NOT EXISTS id_reason BIGINT;
ALTER TABLE credit_note
ADD CONSTRAINT credit_note_reason_fk FOREIGN KEY (id_reason) REFERENCES credit_note_reason(id_reason);
COMMENT ON COLUMN credit_note.id_reason IS 'ID del motivo de la nota de crédito';
-- 5. Agregar campos adicionales a credit_note_detail
ALTER TABLE credit_note_detail
ADD COLUMN IF NOT EXISTS id_invoice_detail BIGINT;
ALTER TABLE credit_note_detail
ADD COLUMN IF NOT EXISTS main_code VARCHAR(25);
ALTER TABLE credit_note_detail
ADD COLUMN IF NOT EXISTS aux_code VARCHAR(25);
ALTER TABLE credit_note_detail
ADD COLUMN IF NOT EXISTS description VARCHAR(300);
ALTER TABLE credit_note_detail
ADD COLUMN IF NOT EXISTS iva_percentage NUMERIC(5, 2) DEFAULT 0;
ALTER TABLE credit_note_detail
ADD COLUMN IF NOT EXISTS iva_value NUMERIC(14, 2) DEFAULT 0;
ALTER TABLE credit_note_detail
ADD COLUMN IF NOT EXISTS ice_value NUMERIC(14, 2) DEFAULT 0;
-- FK a invoice_detail (opcional, puede ser null si es producto nuevo)
ALTER TABLE credit_note_detail
ADD CONSTRAINT credit_note_detail_invoice_detail_fk FOREIGN KEY (id_invoice_detail) REFERENCES invoice_detail(id_invoice_detail);
COMMENT ON COLUMN credit_note_detail.id_invoice_detail IS 'ID del detalle de factura original (para tracking)';
COMMENT ON COLUMN credit_note_detail.main_code IS 'Código principal del producto';
COMMENT ON COLUMN credit_note_detail.aux_code IS 'Código auxiliar del producto';
COMMENT ON COLUMN credit_note_detail.description IS 'Descripción del producto/servicio';
COMMENT ON COLUMN credit_note_detail.iva_percentage IS 'Porcentaje de IVA aplicado';
COMMENT ON COLUMN credit_note_detail.iva_value IS 'Valor del IVA calculado';
COMMENT ON COLUMN credit_note_detail.ice_value IS 'Valor del ICE calculado';
-- 6. Agregar campos adicionales a credit_note si no existen
ALTER TABLE credit_note
ADD COLUMN IF NOT EXISTS total_iva NUMERIC(14, 2) DEFAULT 0;
ALTER TABLE credit_note
ADD COLUMN IF NOT EXISTS total_ice NUMERIC(14, 2) DEFAULT 0;
ALTER TABLE credit_note
ADD COLUMN IF NOT EXISTS tip NUMERIC(14, 2) DEFAULT 0;
ALTER TABLE credit_note
ADD COLUMN IF NOT EXISTS observation TEXT;
COMMENT ON COLUMN credit_note.total_iva IS 'Total de IVA de la nota de crédito';
COMMENT ON COLUMN credit_note.total_ice IS 'Total de ICE de la nota de crédito';
COMMENT ON COLUMN credit_note.tip IS 'Propina';
COMMENT ON COLUMN credit_note.observation IS 'Observación adicional';
-- 7. Crear índices para optimizar consultas
CREATE INDEX IF NOT EXISTS idx_credit_note_reason ON credit_note(id_reason);
CREATE INDEX IF NOT EXISTS idx_credit_note_detail_invoice_detail ON credit_note_detail(id_invoice_detail);