-- ============================================
-- Script: Assign administrative feature menus to super_admin
-- Date: 2025-12-18
-- Purpose: Grant access to ROLE_PERMISSION_MANAGEMENT and SUBSCRIPTION_MANAGEMENT menus
-- Only super_admin (role 1) gets these menus
-- Uses helper functions for safe, idempotent execution
-- ============================================

-- ============================================
-- ASSIGN MENUS TO SUPER_ADMIN (role 1)
-- ============================================

DO $$
DECLARE
    menu_id INTEGER;
    assignment_id BIGINT;
BEGIN
    -- Find menu by feature_code: ROLE_PERMISSION_MANAGEMENT
    menu_id := get_menu_id_by_feature_code('ROLE_PERMISSION_MANAGEMENT');
    
    IF menu_id IS NOT NULL THEN
        assignment_id := assign_menu_to_role_safe(menu_id, 1);
        RAISE NOTICE '✅ Menu "Gestión de Permisos" (ID: %) assigned to super_admin - Assignment ID: %', menu_id, assignment_id;
    ELSE
        RAISE WARNING '⚠️ Menu with feature_code ROLE_PERMISSION_MANAGEMENT not found';
    END IF;
    
    -- Find menu by feature_code: SUBSCRIPTION_MANAGEMENT
    menu_id := get_menu_id_by_feature_code('SUBSCRIPTION_MANAGEMENT');
    
    IF menu_id IS NOT NULL THEN
        assignment_id := assign_menu_to_role_safe(menu_id, 1);
        RAISE NOTICE '✅ Menu "Gestión de Suscripciones" (ID: %) assigned to super_admin - Assignment ID: %', menu_id, assignment_id;
    ELSE
        RAISE WARNING '⚠️ Menu with feature_code SUBSCRIPTION_MANAGEMENT not found';
    END IF;
END $$;

-- ============================================
-- VERIFICATION QUERIES (commented - run manually)
-- ============================================

-- Verify menu assignments for super_admin
-- SELECT mr.id_menu_role, mr.id_menu, m.name, mr.id_role, m.feature_code
-- FROM public.menu_role mr
-- INNER JOIN public.menu m ON m.id_menu = mr.id_menu
-- WHERE mr.id_role = 1 
--   AND mr.id_menu IN (32, 33)
--   AND mr.status = true;

-- Expected output: 2 rows
-- Should show both menus (32, 33) assigned to role 1

-- Count all menus for super_admin (should be ~33 now)
-- SELECT COUNT(*) as total_menus_super_admin
-- FROM public.menu_role 
-- WHERE id_role = 1 AND status = true;

-- List all menus under "Administración Global" for super_admin
-- SELECT m.id_menu, m.name, m.url, m.feature_code
-- FROM public.menu m
-- INNER JOIN public.menu_role mr ON mr.id_menu = m.id_menu
-- WHERE m.parent_id_menu = 16 
--   AND mr.id_role = 1
--   AND m.status = true
--   AND mr.status = true
-- ORDER BY m.id_menu;
