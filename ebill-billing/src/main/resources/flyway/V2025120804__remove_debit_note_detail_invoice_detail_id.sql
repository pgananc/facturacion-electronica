-- Eliminar columna id_invoice_detail de la tabla debit_note_detail
-- Este campo no es necesario para notas de débito ya que pueden agregar productos nuevos
-- que no existían en la factura original (intereses, recargos, etc.)

ALTER TABLE debit_note_detail
DROP COLUMN IF EXISTS id_invoice_detail;






