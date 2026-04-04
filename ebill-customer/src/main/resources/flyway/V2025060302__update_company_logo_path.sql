-- Agregar columna logo_path para almacenar la ruta completa del logo
ALTER TABLE public.company
ADD COLUMN logo_path varchar(500);
-- Eliminar columna logo_file (ya no se necesita, los logos se guardan en el sistema de archivos)
ALTER TABLE public.company DROP COLUMN IF EXISTS logo_file;