
-- Motivo 1: Intereses por mora
INSERT INTO public.product (id_product, creation_date, description, id_company, main_code, name, product_type, status, unit_price, update_date, has_ice, has_tourism_iva,id_iva_tax_type) 
VALUES ((SELECT COALESCE(MAX(id_product), 0) + 1 FROM public.product), '2025-12-22 14:30:00.000000', 'Intereses generados por mora en pago', 1, 'ND-INT', 'INTERESES POR MORA', 2, true, 0.00, null, false, false,5);

-- Motivo 2: Diferencia de precio
INSERT INTO public.product (id_product, creation_date, description, id_company, main_code, name, product_type, status, unit_price, update_date, has_ice, has_tourism_iva,id_iva_tax_type) 
VALUES ((SELECT COALESCE(MAX(id_product), 0) + 1 FROM public.product), '2025-12-22 14:30:00.000000', 'Ajuste por diferencia en precio facturado', 1, 'ND-DIP', 'DIFERENCIA DE PRECIO', 2, true, 0.00, null, false, false,5);

-- Motivo 3: Diferencia de cantidad
INSERT INTO public.product (id_product, creation_date, description, id_company, main_code, name, product_type, status, unit_price, update_date, has_ice, has_tourism_iva,id_iva_tax_type) 
VALUES ((SELECT COALESCE(MAX(id_product), 0) + 1 FROM public.product), '2025-12-22 14:30:00.000000', 'Ajuste por diferencia en cantidad entregada', 1, 'ND-DIC', 'DIFERENCIA DE CANTIDAD', 2, true, 0.00, null, false, false,5);

-- Motivo 4: Gastos de envío / fletes
INSERT INTO public.product (id_product, creation_date, description, id_company, main_code, name, product_type, status, unit_price, update_date, has_ice, has_tourism_iva,id_iva_tax_type) 
VALUES ((SELECT COALESCE(MAX(id_product), 0) + 1 FROM public.product), '2025-12-22 14:30:00.000000', 'Cobro de fletes y logística post-facturación', 1, 'ND-FLE', 'GASTOS DE ENVIO / FLETES', 2, true, 0.00, null, false, false,5);

-- Motivo 5: Otros recargos
INSERT INTO public.product (id_product, creation_date, description, id_company, main_code, name, product_type, status, unit_price, update_date, has_ice, has_tourism_iva,id_iva_tax_type) 
VALUES ((SELECT COALESCE(MAX(id_product), 0) + 1 FROM public.product), '2025-12-22 14:30:00.000000', 'Otros recargos administrativos', 1, 'ND-OTR', 'OTROS RECARGOS', 2, true, 0.00, null, false, false,5);