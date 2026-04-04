-- Crear secuencia para credit_note
CREATE SEQUENCE IF NOT EXISTS credit_note_seq START WITH 1 INCREMENT BY 1;
-- Crear secuencia para credit_note_detail
CREATE SEQUENCE IF NOT EXISTS credit_note_detail_seq START WITH 1 INCREMENT BY 1;
-- Crear tabla credit_note
CREATE TABLE credit_note (
    id_credit_note BIGINT NOT NULL PRIMARY KEY,
    id_company BIGINT,
    id_client BIGINT,
    id_issuance_point BIGINT,
    sequential BIGINT,
    retry_count INTEGER,
    issue_date TIMESTAMP,
    credit_note_number VARCHAR(255),
    environment VARCHAR(255),
    additional_information VARCHAR(255),
    total_amount NUMERIC(38, 2),
    subtotal_zero NUMERIC(38, 2),
    subtotal_iva NUMERIC(38, 2),
    subtotal_ice NUMERIC(38, 2),
    discount NUMERIC(38, 2),
    valor_modificacion NUMERIC(38, 2),
    email_sent BOOLEAN,
    email_sent_date TIMESTAMP,
    -- Campos específicos de Nota de Crédito
    original_invoice_id BIGINT,
    original_access_key VARCHAR(49),
    original_invoice_number VARCHAR(15),
    original_issue_date TIMESTAMP,
    document_type_modified VARCHAR(2),
    reason VARCHAR(300),
    -- Foreign keys
    id_authorization_sri BIGINT CONSTRAINT credit_note_authorization_sri_id_authorization_sri_fk REFERENCES authorization_sri,
    status BOOLEAN,
    creation_date TIMESTAMP NOT NULL,
    update_date TIMESTAMP
);
COMMENT ON TABLE credit_note IS 'Tabla de notas de crédito';
COMMENT ON COLUMN credit_note.original_invoice_id IS 'ID de la factura original que se modifica/anula';
COMMENT ON COLUMN credit_note.original_access_key IS 'Clave de acceso de la factura original (49 dígitos)';
COMMENT ON COLUMN credit_note.original_invoice_number IS 'Número de factura original (formato: estab-ptoEmi-secuencial, 15 caracteres)';
COMMENT ON COLUMN credit_note.original_issue_date IS 'Fecha de emisión de la factura original';
COMMENT ON COLUMN credit_note.document_type_modified IS 'Tipo de documento modificado (01=Factura, 03=Liquidación, etc.)';
COMMENT ON COLUMN credit_note.reason IS 'Motivo de la nota de crédito (obligatorio, max 300 caracteres)';
COMMENT ON COLUMN credit_note.valor_modificacion IS 'Valor total de la modificación';
-- Crear tabla credit_note_detail
CREATE TABLE credit_note_detail (
    id_credit_note_detail BIGINT NOT NULL PRIMARY KEY,
    id_credit_note BIGINT CONSTRAINT credit_note_detail_credit_note_id_credit_note_fk REFERENCES credit_note,
    id_product BIGINT,
    unit_price NUMERIC(38, 2),
    discount NUMERIC(38, 2),
    subtotal NUMERIC(38, 2),
    quantity INTEGER,
    status BOOLEAN,
    creation_date TIMESTAMP NOT NULL,
    update_date TIMESTAMP
);
COMMENT ON TABLE credit_note_detail IS 'Tabla de detalles de notas de crédito';

