-- Agregar columna regime para almacenar el régimen del contribuyente
ALTER TABLE public.company
ADD COLUMN regime varchar(50);

-- Comentario sobre los valores posibles
COMMENT ON COLUMN public.company.regime IS 'Régimen del contribuyente: GENERAL, RIMPE_POPULAR, RIMPE_STARTUP, SPECIAL';

-- Agregar columna special_taxpayer_resolution para almacenar la resolución del contribuyente especial
ALTER TABLE public.company
ADD COLUMN special_taxpayer_resolution varchar(255);

-- Comentario sobre la columna
COMMENT ON COLUMN public.company.special_taxpayer_resolution IS 'Resolución del contribuyente especial';


