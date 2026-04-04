-- Eliminar la columna id_payment_method de la tabla invoice
-- Esta columna es redundante porque los métodos de pago ahora se almacenan en invoice_payment
-- a través de la relación OneToMany invoicePayments
-- Primero eliminar la foreign key constraint si existe
ALTER TABLE invoice DROP CONSTRAINT IF EXISTS invoice_payment_method_id_payment_method_fk;
-- Eliminar la columna id_payment_method
ALTER TABLE invoice DROP COLUMN IF EXISTS id_payment_method;
COMMENT ON TABLE invoice IS 'Tabla de facturas. Los métodos de pago se almacenan en invoice_payment';