-- Paso 1: Agregar columna nullable
ALTER TABLE public.company
    ADD COLUMN logo_file            BYTEA;
ALTER TABLE public.company
    ADD COLUMN rimpe_registered_taxpayer   boolean;
ALTER TABLE public.company
    ADD COLUMN small_scale_informal_business  boolean;
ALTER TABLE public.company
    ADD COLUMN tax_withholding_agent boolean;