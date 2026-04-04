-- ============================================
-- Script: Assign menus to Admin role (id=3)
-- Date: 2025-12-17
-- Purpose: Configure menu permissions for Admin role
-- Admin can access all company-level features but NOT global administration
-- ============================================

-- ============================================
-- ADMIN ROLE (id=3) - MENU ASSIGNMENTS
-- Total: 28 menus (7 parent groups + 21 submenus)
-- ============================================

-- ============================================
-- FIX: Sync menu_role_seq with existing data
-- ============================================

-- Get current max id_menu_role and set sequence to that value + 1
DO $$
DECLARE
    max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id_menu_role), 0) INTO max_id FROM public.menu_role;
    PERFORM setval('menu_role_seq', max_id);
END $$;

-- ============================================
-- PARENT MENUS (7 groups)
-- ============================================

-- Menu ID 1: Cliente (Parent group)
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 1, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 1 AND id_role = 3
);

-- Menu ID 2: Producto (Parent group)
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 2, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 2 AND id_role = 3
);

-- Menu ID 3: Compañía (Parent group)
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 3, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 3 AND id_role = 3
);

-- Menu ID 4: Usuario (Parent group)
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 4, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 4 AND id_role = 3
);

-- Menu ID 5: Facturación Electrónica (Parent group)
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 5, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 5 AND id_role = 3
);

-- Menu ID 20: Reportes (Parent group)
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 20, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 20 AND id_role = 3
);

-- ❌ Menu ID 16: Administración Global (Parent group) - EXCLUDED for Admin role
-- This is only for super_admin (id=1)

-- ============================================
-- SUBMENUS - Cliente (1 submenu)
-- ============================================

-- Menu ID 8: Gestionar clientes
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 8, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 8 AND id_role = 3
);

-- ============================================
-- SUBMENUS - Producto (1 submenu)
-- ============================================

-- Menu ID 9: Gestionar productos
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 9, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 9 AND id_role = 3
);

-- ============================================
-- SUBMENUS - Compañía (3 submenus)
-- ============================================

-- Menu ID 10: Gestionar compañía
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 10, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 10 AND id_role = 3
);

-- Menu ID 14: Gestionar sucursal
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 14, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 14 AND id_role = 3
);

-- Menu ID 15: Gestionar Punto de Emisión
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 15, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 15 AND id_role = 3
);

-- ============================================
-- SUBMENUS - Usuario (2 submenus)
-- ============================================

-- Menu ID 7: Gestionar usuarios por compañía
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 7, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 7 AND id_role = 3
);

-- Menu ID 11: Gestionar usuario
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 11, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 11 AND id_role = 3
);

-- ============================================
-- SUBMENUS - Facturación Electrónica (3 submenus)
-- ============================================

-- Menu ID 6: Facturas
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 6, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 6 AND id_role = 3
);

-- Menu ID 12: Borradores
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 12, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 12 AND id_role = 3
);

-- Menu ID 19: Nota de crédito
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 19, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 19 AND id_role = 3
);

-- ============================================
-- SUBMENUS - Reportes (11 submenus)
-- ============================================

-- Menu ID 21: Reportes operativos (container)
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 21, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 21 AND id_role = 3
);

-- Menu ID 22: Ventas por periodo
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 22, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 22 AND id_role = 3
);

-- Menu ID 23: Ventas por cliente
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 23, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 23 AND id_role = 3
);

-- Menu ID 24: Ventas por producto
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 24, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 24 AND id_role = 3
);

-- Menu ID 25: Reporte de notas de crédito
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 25, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 25 AND id_role = 3
);

-- Menu ID 26: Reporte Novedades (container)
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 26, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 26 AND id_role = 3
);

-- Menu ID 27: Reporte de facturas pendientes de envío
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 27, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 27 AND id_role = 3
);

-- Menu ID 28: Reportes comerciales (container)
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 28, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 28 AND id_role = 3
);

-- Menu ID 29: Dashboard de ventas
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 29, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 29 AND id_role = 3
);

-- Menu ID 30: Comparativo por sucursal / punto de emisión
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 30, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 30 AND id_role = 3
);

-- Menu ID 31: Reporte de formas de pago
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT nextval('menu_role_seq'), 31, 3, true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role WHERE id_menu = 31 AND id_role = 3
);

-- ============================================
-- VERIFICATION QUERIES (commented - run manually for testing)
-- ============================================

-- Count total menus assigned to Admin role
-- SELECT COUNT(*) as total_menus_admin 
-- FROM public.menu_role 
-- WHERE id_role = 3 AND status = true;
-- Expected: 28

-- List all menus assigned to Admin role
-- SELECT mr.id_menu, m.name, m.url, m.parent_id_menu
-- FROM public.menu_role mr
-- INNER JOIN public.menu m ON m.id_menu = mr.id_menu
-- WHERE mr.id_role = 3 AND mr.status = true
-- ORDER BY m.parent_id_menu NULLS FIRST, m.id_menu;

-- Verify Admin does NOT have access to Administración Global
-- SELECT mr.id_menu, m.name
-- FROM public.menu_role mr
-- INNER JOIN public.menu m ON m.id_menu = mr.id_menu
-- WHERE mr.id_role = 3 
--   AND mr.status = true
--   AND m.id_menu IN (16, 17, 18);
-- Expected: 0 rows
