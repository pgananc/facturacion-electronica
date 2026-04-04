-- Crear secuencia para debit_note
CREATE SEQUENCE IF NOT EXISTS debit_note_seq START WITH 1 INCREMENT BY 1;
-- Crear secuencia para debit_note_detail
CREATE SEQUENCE IF NOT EXISTS debit_note_detail_seq START WITH 1 INCREMENT BY 1;
-- Crear secuencia para debit_note_motivo
CREATE SEQUENCE IF NOT EXISTS debit_note_motivo_seq START WITH 1 INCREMENT BY 1;
-- Crear secuencia para debit_note_payment
CREATE SEQUENCE IF NOT EXISTS debit_note_payment_seq START WITH 1 INCREMENT BY 1;

-- Crear tabla debit_note
CREATE TABLE debit_note (
    id_debit_note BIGINT NOT NULL PRIMARY KEY,
    id_company BIGINT,
    id_client BIGINT,
    id_establishment BIGINT,
    id_issuance_point BIGINT,
    sequential BIGINT,
    retry_count INTEGER DEFAULT 0,
    issue_date TIMESTAMP NOT NULL,
    debit_note_number VARCHAR(17),
    environment VARCHAR(10),
    additional_information TEXT,
    total_amount NUMERIC(19, 2) NOT NULL,
    subtotal_zero NUMERIC(19, 2) DEFAULT 0,
    subtotal_iva NUMERIC(19, 2) DEFAULT 0,
    subtotal_ice NUMERIC(19, 2) DEFAULT 0,
    discount NUMERIC(19, 2) DEFAULT 0,
    total_iva NUMERIC(19, 2) DEFAULT 0,
    total_ice NUMERIC(19, 2) DEFAULT 0,
    valor_modificacion NUMERIC(19, 2) NOT NULL,
    valor_total NUMERIC(19, 2) NOT NULL,
    observation TEXT,
    email_sent BOOLEAN DEFAULT FALSE,
    email_sent_date TIMESTAMP,
    -- Campos específicos de Nota de Débito
    original_invoice_id BIGINT NOT NULL,
    original_access_key VARCHAR(49),
    original_invoice_number VARCHAR(17) NOT NULL,
    original_issue_date TIMESTAMP NOT NULL,
    document_type_modified VARCHAR(2) DEFAULT '01',
    id_reason BIGINT,
    reason VARCHAR(300),
    -- Foreign keys
    id_authorization_sri BIGINT CONSTRAINT debit_note_authorization_sri_id_authorization_sri_fk REFERENCES authorization_sri,
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_debit_note_invoice FOREIGN KEY (original_invoice_id) REFERENCES invoice(id_invoice)
);

COMMENT ON TABLE debit_note IS 'Tabla de notas de débito';
COMMENT ON COLUMN debit_note.original_invoice_id IS 'ID de la factura original que se modifica';
COMMENT ON COLUMN debit_note.original_access_key IS 'Clave de acceso de la factura original (49 dígitos)';
COMMENT ON COLUMN debit_note.original_invoice_number IS 'Número de factura original (formato: estab-ptoEmi-secuencial, 17 caracteres)';
COMMENT ON COLUMN debit_note.original_issue_date IS 'Fecha de emisión de la factura original';
COMMENT ON COLUMN debit_note.document_type_modified IS 'Tipo de documento modificado (01=Factura, 03=Liquidación, etc.)';
COMMENT ON COLUMN debit_note.reason IS 'Motivo general de la nota de débito (opcional si hay motivos detallados)';
COMMENT ON COLUMN debit_note.valor_modificacion IS 'Valor total de la modificación (suma de motivos + impuestos)';
COMMENT ON COLUMN debit_note.valor_total IS 'Valor total para el SRI (mismo que valorModificacion)';

-- Crear tabla debit_note_detail
CREATE TABLE debit_note_detail (
    id_debit_note_detail BIGINT NOT NULL PRIMARY KEY,
    id_debit_note BIGINT NOT NULL CONSTRAINT debit_note_detail_debit_note_id_debit_note_fk REFERENCES debit_note ON DELETE CASCADE,
    id_product BIGINT,
    id_invoice_detail BIGINT,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    discount NUMERIC(19, 2) DEFAULT 0,
    subtotal NUMERIC(19, 2) NOT NULL,
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE debit_note_detail IS 'Tabla de detalles de notas de débito';
COMMENT ON COLUMN debit_note_detail.id_invoice_detail IS 'Referencia al detalle de factura original (opcional)';

-- Crear tabla debit_note_motivo
CREATE TABLE debit_note_motivo (
    id_debit_note_motivo BIGINT NOT NULL PRIMARY KEY,
    id_debit_note BIGINT NOT NULL CONSTRAINT debit_note_motivo_debit_note_id_debit_note_fk REFERENCES debit_note ON DELETE CASCADE,
    razon VARCHAR(500) NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE debit_note_motivo IS 'Tabla de motivos detallados de notas de débito';
COMMENT ON COLUMN debit_note_motivo.razon IS 'Descripción del motivo';
COMMENT ON COLUMN debit_note_motivo.valor IS 'Monto del motivo';

-- Crear tabla debit_note_payment
CREATE TABLE debit_note_payment (
    id_debit_note_payment BIGINT NOT NULL PRIMARY KEY,
    id_debit_note BIGINT NOT NULL CONSTRAINT debit_note_payment_debit_note_id_debit_note_fk REFERENCES debit_note ON DELETE CASCADE,
    id_payment_method BIGINT NOT NULL CONSTRAINT debit_note_payment_method_id_payment_method_fk REFERENCES payment_method,
    forma_pago VARCHAR(2) NOT NULL,
    total NUMERIC(19, 2) NOT NULL,
    term INTEGER,
    time_unit VARCHAR(10),
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE debit_note_payment IS 'Tabla de formas de pago de notas de débito';
COMMENT ON COLUMN debit_note_payment.forma_pago IS 'Código SRI de forma de pago (01, 16, 19, etc.)';
COMMENT ON COLUMN debit_note_payment.total IS 'Monto del pago';
COMMENT ON COLUMN debit_note_payment.term IS 'Plazo de pago (opcional)';
COMMENT ON COLUMN debit_note_payment.time_unit IS 'Unidad de tiempo del plazo (opcional)';

-- Crear índices
CREATE INDEX idx_debit_note_issuance_point ON debit_note(id_issuance_point, sequential);
CREATE INDEX idx_debit_note_original_invoice ON debit_note(original_invoice_id);
CREATE INDEX idx_debit_note_authorization ON debit_note(id_authorization_sri);
CREATE INDEX idx_debit_note_detail_debit_note ON debit_note_detail(id_debit_note);
CREATE INDEX idx_debit_note_motivo_debit_note ON debit_note_motivo(id_debit_note);
CREATE INDEX idx_debit_note_payment_debit_note ON debit_note_payment(id_debit_note);

