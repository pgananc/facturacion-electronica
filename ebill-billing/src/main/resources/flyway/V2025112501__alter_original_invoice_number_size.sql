-- Modificar el tamaño de la columna original_invoice_number de VARCHAR(15) a VARCHAR(17)
-- Esto permite almacenar números de factura más largos si es necesario
ALTER TABLE credit_note
ALTER COLUMN original_invoice_number TYPE VARCHAR(17);
-- Actualizar el comentario de la columna
COMMENT ON COLUMN credit_note.original_invoice_number IS 'Número de factura original (formato: estab-ptoEmi-secuencial, 17 caracteres)';

