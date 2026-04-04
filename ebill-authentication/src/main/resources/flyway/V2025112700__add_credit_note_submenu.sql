-- Agregar submenú "Nota de crédito" bajo "Facturación Electrónica" (ID: 5)
-- Renombrar "Gestionar Sucursal" a "Gestionar sucursal" (corrección de mayúscula)
-- Con permisos para super_admin (rol 1)

-- Insertar el nuevo submenú "Nota de crédito" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 19, 5, 'credit_card', 'Nota de crédito', '/credit-note', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 19);

-- Asignar permisos de "Nota de crédito" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 28, 19, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 28);

-- Renombrar "Gestionar Sucursal" a "Gestionar sucursal" (corrección de mayúscula)
UPDATE public.menu
SET name = 'Gestionar sucursal'
WHERE id_menu = 14 AND name = 'Gestionar Sucursal';

