-- Add issuance point and sequential columns to invoice table
ALTER TABLE invoice
ADD COLUMN id_issuance_point bigint,
ADD COLUMN sequential bigint;

-- Add comment to explain the sequential field
COMMENT ON COLUMN invoice.sequential IS 'Sequential number for each issuance point, starts from 1';
COMMENT ON COLUMN invoice.id_issuance_point IS 'Foreign key to issuance point (from ebill-core service)';

-- Create index for better query performance
CREATE INDEX idx_invoice_issuance_point_sequential ON invoice(id_issuance_point, sequential);

