-- Agregar nuevo menú padre "Administración Global" con submenús:
-- - Gestión Global de Sucursales
-- - Gestión Global de Puntos de Emisión
-- Con permisos para super_admin (rol 1)

-- Insertar el nuevo menú padre "Administración Global" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 16, NULL, 'admin_panel_settings', 'Administración Global', NULL, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 16);

-- Insertar submenú "Gestión Global de Sucursales" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 17, 16, 'storefront', 'Gestión Global de Sucursales', '/admin/establishments', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 17);

-- Insertar submenú "Gestión Global de Puntos de Emisión" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 18, 16, 'receipt', 'Gestión Global de Puntos de Emisión', '/admin/invoice-points', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 18);

-- Asignar permisos del menú padre "Administración Global" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 25, 16, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 25);

-- Asignar permisos de "Gestión Global de Sucursales" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 26, 17, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 26);

-- Asignar permisos de "Gestión Global de Puntos de Emisión" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 27, 18, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 27);

