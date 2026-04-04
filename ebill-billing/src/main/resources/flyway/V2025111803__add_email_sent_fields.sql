-- Agregar campos para rastrear el envío de correos
ALTER TABLE invoice
ADD COLUMN email_sent BOOLEAN DEFAULT FALSE,
    ADD COLUMN email_sent_date TIMESTAMP;
-- Crear índice para consultas rápidas de facturas no enviadas
CREATE INDEX IF NOT EXISTS idx_invoice_email_sent ON invoice(email_sent, id_authorization_sri);
-- Actualizar facturas existentes que ya tienen autorización
-- (asumiendo que si tienen autorización, probablemente ya se enviaron)
UPDATE invoice
SET email_sent = TRUE,
    email_sent_date = authorization_sri.authorization_date
FROM authorization_sri
WHERE invoice.id_authorization_sri = authorization_sri.id_authorization_sri
    AND authorization_sri.authorization_status_sri = 'AUTORIZADO'
    AND invoice.email_sent IS NULL;