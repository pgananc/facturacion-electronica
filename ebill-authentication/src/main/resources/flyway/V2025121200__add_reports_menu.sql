-- Agregar nuevo menú padre "Reportes" con submenús:
-- - Reportes operativos
-- - Reporte Novedades
-- - Reportes comerciales
-- Con permisos para super_admin (rol 1) y asignados al usuario 1

-- Insertar el nuevo menú padre "Reportes" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 20, NULL, 'assessment', 'Reportes', '/reports', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 20);

-- Insertar submenú "Reportes operativos" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 21, 20, 'analytics', 'Reportes operativos', '/reports/operational', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 21);

-- Insertar submenú "Reporte Novedades" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 22, 20, 'new_releases', 'Reporte Novedades', '/reports/news', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 22);

-- Insertar submenú "Reportes comerciales" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 23, 20, 'trending_up', 'Reportes comerciales', '/reports/sales', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 23);

-- Asignar permisos del menú padre "Reportes" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 29, 20, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 29);

-- Asignar permisos de "Reportes operativos" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 30, 21, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 30);

-- Asignar permisos de "Reporte Novedades" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 31, 22, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 31);

-- Asignar permisos de "Reportes comerciales" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 32, 23, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 32);

