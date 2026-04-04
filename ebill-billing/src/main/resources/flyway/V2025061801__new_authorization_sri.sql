-- Paso 1: Agregar columnas en authorization_sri
ALTER TABLE public.authorization_sri
    RENAME status_sri TO authorization_status_sri;
ALTER TABLE public.authorization_sri
    ADD COLUMN reception_status_sri character varying(255); 
ALTER TABLE public.authorization_sri
    ADD COLUMN reception_error text;
ALTER TABLE public.authorization_sri
    RENAME error TO authorization_error;
ALTER TABLE public.authorization_sri
    ADD COLUMN reception_xml text; 
ALTER TABLE public.authorization_sri
    RENAME xml TO authorization_xml;
