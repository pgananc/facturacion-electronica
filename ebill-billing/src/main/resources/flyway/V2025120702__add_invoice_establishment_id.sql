ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS id_establishment bigint;
ALTER TABLE credit_note
    ADD COLUMN IF NOT EXISTS id_establishment bigint;








