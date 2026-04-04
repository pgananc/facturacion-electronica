-- Paso 1: Agregar columna nullable
ALTER TABLE public.product
    ADD COLUMN id_tax_type bigint;

-- Paso 2: Asignar el tax_type al producto existente
UPDATE public.product
SET id_tax_type = 5
WHERE id_tax_type IS NULL;

-- Paso 3: Cambiar columna a NOT NULL
ALTER TABLE public.product
    ALTER COLUMN id_tax_type SET NOT NULL;

-- Paso 4: Agregar la clave foránea
ALTER TABLE public.product
    ADD CONSTRAINT fk_product_tax_type
        FOREIGN KEY (id_tax_type)
            REFERENCES public.tax_type(id_tax_type);
