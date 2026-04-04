-- Eliminar columna special_taxpayer ya que no se necesita más
ALTER TABLE public.company
DROP COLUMN IF EXISTS special_taxpayer;


