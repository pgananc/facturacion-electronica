-- Agregar menú "Reportes" con sus submenús:
-- - Reportes operativos (con 4 submenús)
-- - Reporte Novedades (con 1 submenú)
-- - Reportes comerciales (con 3 submenús)
-- Con permisos para super_admin (rol 1)

-- Insertar el menú principal "Reportes" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 20, NULL, 'assessment', 'Reportes', NULL, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 20);

-- Insertar submenú "Reportes operativos" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 21, 20, 'analytics', 'Reportes operativos', NULL, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 21);

-- Insertar submenú "Ventas por periodo" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 22, 21, 'date_range', 'Ventas por periodo', 'sales-by-period', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 22);

-- Insertar submenú "Ventas por cliente" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 23, 21, 'people', 'Ventas por cliente', 'sales-by-client', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 23);

-- Insertar submenú "Ventas por producto" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 24, 21, 'inventory_2', 'Ventas por producto', 'sales-by-product', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 24);

-- Insertar submenú "Reporte de notas de crédito" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 25, 21, 'receipt_long', 'Reporte de notas de crédito', 'credit-notes', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 25);

-- Insertar submenú "Reporte Novedades" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 26, 20, 'notifications_active', 'Reporte Novedades', NULL, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 26);

-- Insertar submenú "Reporte de facturas pendientes de envío" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 27, 26, 'pending_actions', 'Reporte de facturas pendientes de envío', 'pending-invoices', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 27);

-- Insertar submenú "Reportes comerciales" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 28, 20, 'business_center', 'Reportes comerciales', NULL, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 28);

-- Insertar submenú "Dashboard de ventas" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 29, 28, 'dashboard', 'Dashboard de ventas', 'sales-dashboard', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 29);

-- Insertar submenú "Comparativo por sucursal / punto de emisión" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 30, 28, 'compare_arrows', 'Comparativo por sucursal / punto de emisión', 'branch-comparison', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 30);

-- Insertar submenú "Reporte de formas de pago" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 31, 28, 'payment', 'Reporte de formas de pago', 'payment-methods', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 31);

-- Asignar permisos del menú principal "Reportes" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 29, 20, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 29);

-- Asignar permisos de "Reportes operativos" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 30, 21, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 30);

-- Asignar permisos de "Ventas por periodo" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 31, 22, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 31);

-- Asignar permisos de "Ventas por cliente" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 32, 23, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 32);

-- Asignar permisos de "Ventas por producto" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 33, 24, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 33);

-- Asignar permisos de "Reporte de notas de crédito" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 34, 25, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 34);

-- Asignar permisos de "Reporte Novedades" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 35, 26, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 35);

-- Asignar permisos de "Reporte de facturas pendientes de envío" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 36, 27, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 36);

-- Asignar permisos de "Reportes comerciales" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 37, 28, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 37);

-- Asignar permisos de "Dashboard de ventas" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 38, 29, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 38);

-- Asignar permisos de "Comparativo por sucursal / punto de emisión" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 39, 30, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 39);

-- Asignar permisos de "Reporte de formas de pago" para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 40, 31, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 40);

