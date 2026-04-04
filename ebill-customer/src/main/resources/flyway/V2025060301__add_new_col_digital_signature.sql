-- Paso 1: Agregar columna nullable
ALTER TABLE public.digital_signature
    ADD COLUMN name_file varchar(255);