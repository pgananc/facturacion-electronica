-- Crear tabla para almacenar las formas de pago de cada factura
CREATE SEQUENCE IF NOT EXISTS invoice_payment_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE invoice_payment (
    id_invoice_payment BIGINT NOT NULL PRIMARY KEY,
    id_invoice BIGINT NOT NULL,
    id_payment_method BIGINT,
    total NUMERIC(38, 2),
    term INTEGER,
    time_unit VARCHAR(50),
    status BOOLEAN,
    creation_date TIMESTAMP NOT NULL,
    update_date TIMESTAMP,
    CONSTRAINT invoice_payment_invoice_fk FOREIGN KEY (id_invoice) REFERENCES invoice(id_invoice) ON DELETE CASCADE,
    CONSTRAINT invoice_payment_payment_method_fk FOREIGN KEY (id_payment_method) REFERENCES payment_method(id_payment_method)
);
COMMENT ON TABLE invoice_payment IS 'Formas de pago asociadas a cada factura';
COMMENT ON COLUMN invoice_payment.id_invoice IS 'ID de la factura a la que pertenece esta forma de pago';
COMMENT ON COLUMN invoice_payment.id_payment_method IS 'Referencia al método de pago (el código se obtiene desde payment_method.payment_method_code)';
COMMENT ON COLUMN invoice_payment.total IS 'Monto correspondiente a esta forma de pago';
COMMENT ON COLUMN invoice_payment.term IS 'Plazo de pago';
COMMENT ON COLUMN invoice_payment.time_unit IS 'Unidad de tiempo del plazo (ej: dias, DÍAS)';
-- Índice para mejorar consultas por factura
CREATE INDEX idx_invoice_payment_id_invoice ON invoice_payment(id_invoice);
-- Índice para consultas por método de pago
CREATE INDEX idx_invoice_payment_id_payment_method ON invoice_payment(id_payment_method);
