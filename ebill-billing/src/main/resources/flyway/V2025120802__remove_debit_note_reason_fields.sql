-- Eliminar campos id_reason y reason de la tabla debit_note
-- Estos campos ya no son necesarios porque se usan motivos detallados (debit_note_motivo)

-- Eliminar la foreign key constraint primero
ALTER TABLE debit_note
DROP CONSTRAINT IF EXISTS fk_debit_note_reason;

-- Eliminar las columnas
ALTER TABLE debit_note
DROP COLUMN IF EXISTS id_reason;

ALTER TABLE debit_note
DROP COLUMN IF EXISTS reason;

