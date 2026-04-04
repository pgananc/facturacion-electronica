-- Eliminar tabla debit_note_motivo
-- Los motivos ahora se generan desde los detalles (productos) en lugar de tener una tabla separada

-- Eliminar la tabla y su secuencia
DROP TABLE IF EXISTS debit_note_motivo CASCADE;

DROP SEQUENCE IF EXISTS debit_note_motivo_seq;






