-- Agregar submenu "Borradores" bajo "Facturación Electrónica"
-- Con acceso para roles 1 (Super Admin) y 3 (Administrador)

-- Insertar el nuevo submenu "Borradores" si no existe
INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 12, 5, 'draft', 'Borradores', '/drafts', true
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE id_menu = 12);

-- Asignar permisos para Super Admin (rol 1) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 21, 12, 1, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 21);

-- Asignar permisos para Administrador (rol 3) si no existe
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 22, 12, 3, true
WHERE NOT EXISTS (SELECT 1 FROM public.menu_role WHERE id_menu_role = 22);
