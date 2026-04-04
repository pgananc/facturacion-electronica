-- Agregar submenús al menú "Compañía" existente (ID: 3):
-- - Gestionar Sucursal
-- - Gestionar Punto de Emisión
-- Con permisos para super_admin (rol 1) para que usuarios 1 y 2 puedan acceder

-- Insertar submenú "Gestionar Sucursal" como hijo del menú "Compañía" (ID: 3) si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 14, 3, 'storefront', 'Gestionar Sucursal', '/establishment', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 14);

-- Insertar submenú "Gestionar Punto de Emisión" como hijo del menú "Compañía" (ID: 3) si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 15, 3, 'receipt', 'Gestionar Punto de Emisión', '/invoice-point', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 15);

-- Asignar permisos de "Gestionar Sucursal" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 23, 14, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 23);

-- Asignar permisos de "Gestionar Punto de Emisión" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 24, 15, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 24);

-- Corregir icono del submenú "Borradores" (ID: 12) de 'draft' a 'drafts' (icono válido de Material Design)
UPDATE public.menu
SET icon = 'drafts'
WHERE id_menu = 12 AND icon = 'draft';

