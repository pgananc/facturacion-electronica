-- Agregar campo retry_count a la tabla invoice
ALTER TABLE invoice
    ADD COLUMN retry_count INTEGER DEFAULT 0;

-- Actualizar invoices existentes con retry_count = 0
UPDATE invoice SET retry_count = 0 WHERE retry_count IS NULL;

