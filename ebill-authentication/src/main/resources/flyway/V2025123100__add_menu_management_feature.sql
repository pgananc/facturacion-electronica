-- ============================================
-- Script: Add Menu Management feature and menu
-- Date: 2025-12-31
-- Purpose: Create feature and menu for managing system menus
-- Only super_admin (role 1) has access
-- Uses helper functions for safe, idempotent execution
-- Note: Menu helper functions are defined in V2025122700__create_admin_feature_menus.sql
-- ============================================

-- ============================================
-- STEP 1: Create MENU_MANAGEMENT feature if not exists
-- ============================================

INSERT INTO public.feature_permission (
    feature_code,
    feature_name,
    description,
    module,
    allowed_roles,
    required_plans,
    is_active
)
VALUES (
    'MENU_MANAGEMENT',
    'Gestión de Menús',
    'Permite gestionar y configurar los menús del sistema',
    'ADMINISTRATION',
    ARRAY[1], -- Only super_admin (role 1)
    ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'], -- Available in all plans
    TRUE
)
ON CONFLICT (feature_code) DO NOTHING;

-- ============================================
-- STEP 2: Create menu using helper function
-- Parent: Administración Global (ID: 16)
-- ============================================

DO $$
DECLARE
    menu_id INTEGER;
    assignment_id BIGINT;
BEGIN
    -- Create menu using helper function
    menu_id := insert_menu_safe(
        16,                              -- parent_id_menu: Administración Global
        'Gestión de Menús',              -- name
        '/admin/menus',                  -- url
        'menu',                          -- icon (menu/list icon)
        'MENU_MANAGEMENT'                -- feature_code
    );
    
    RAISE NOTICE '✅ Menu "Gestión de Menús" creado/actualizado - ID: %', menu_id;
    
    -- Assign menu to super_admin (role 1) using helper function
    assignment_id := assign_menu_to_role_safe(menu_id, 1);
    
    RAISE NOTICE '✅ Menu "Gestión de Menús" asignado a super_admin - Assignment ID: %', assignment_id;
END $$;

-- ============================================
-- STEP 3: Verification queries (commented - run manually if needed)
-- ============================================

-- Verify feature was created
-- SELECT feature_code, feature_name, module, allowed_roles, required_plans
-- FROM public.feature_permission
-- WHERE feature_code = 'MENU_MANAGEMENT';

-- Verify menu was created
-- SELECT id_menu, parent_id_menu, name, url, icon, feature_code, status
-- FROM public.menu
-- WHERE name = 'Gestión de Menús';

-- Verify menu assignment to super_admin
-- SELECT mr.id_menu_role, mr.id_menu, m.name, mr.id_role, r.name as role_name
-- FROM public.menu_role mr
-- INNER JOIN public.menu m ON m.id_menu = mr.id_menu
-- INNER JOIN public.role r ON r.id_role = mr.id_role
-- WHERE m.name = 'Gestión de Menús'
--   AND mr.id_role = 1
--   AND mr.status = true;

-- Expected result: Menu assigned to super_admin (role 1)

-- List all menus under "Administración Global" for super_admin
-- SELECT m.id_menu, m.name, m.url, m.feature_code
-- FROM public.menu m
-- INNER JOIN public.menu_role mr ON mr.id_menu = m.id_menu
-- WHERE m.parent_id_menu = 16 
--   AND mr.id_role = 1
--   AND m.status = true
--   AND mr.status = true
-- ORDER BY m.id_menu;
