-- ============================================
-- Script: Add Role Permission Management Menu
-- Date: 2025-12-19
-- Purpose: Ensure menu "Gestión de Permisos" exists with correct configuration
-- This menu allows super_admin to manage role and plan permissions
-- Uses helper functions for safe, idempotent execution
-- ============================================

DO $$
DECLARE
    menu_id INTEGER;
    assignment_id BIGINT;
BEGIN
    -- Insert or update menu using helper function (idempotent)
    menu_id := insert_menu_safe(
        16,                              -- parent_id_menu: Administración Global
        'Gestión de Permisos',          -- name
        '/admin/role-permissions',       -- url
        'security',                      -- icon
        'ROLE_PERMISSION_MANAGEMENT'     -- feature_code
    );
    
    RAISE NOTICE '✅ Menu "Gestión de Permisos" - ID: %', menu_id;
    
    -- Assign to super_admin (role 1) using helper function (idempotent)
    assignment_id := assign_menu_to_role_safe(menu_id, 1);
    
    RAISE NOTICE '✅ Menu assigned to super_admin - Assignment ID: %', assignment_id;
END $$;

-- ============================================
-- VERIFICATION QUERIES (commented - run manually)
-- ============================================

-- Verify menu was created/updated correctly
-- SELECT 
--     m.id_menu,
--     m.parent_id_menu,
--     m.name,
--     m.url,
--     m.icon,
--     m.status,
--     m.feature_code,
--     parent.name AS parent_name
-- FROM public.menu m
-- LEFT JOIN public.menu parent ON m.parent_id_menu = parent.id_menu
-- WHERE m.feature_code = 'ROLE_PERMISSION_MANAGEMENT';

-- Expected output:
-- id_menu: 32 or 41 (depending on what existed)
-- parent_id_menu: 16
-- name: Gestión de Permisos
-- url: /admin/role-permissions
-- icon: security
-- status: true
-- feature_code: ROLE_PERMISSION_MANAGEMENT

-- Verify menu is assigned to super_admin
-- SELECT
--     mr.id_menu_role,
--     r.name AS role_name,
--     m.name AS menu_name,
--     m.url AS menu_url,
--     m.feature_code
-- FROM public.menu_role mr
-- INNER JOIN public.role r ON mr.id_role = r.id_role
-- INNER JOIN public.menu m ON mr.id_menu = m.id_menu
-- WHERE r.id_role = 1
--   AND m.feature_code = 'ROLE_PERMISSION_MANAGEMENT'
--   AND mr.status = true;

-- Verify all menus under Administración Global
-- SELECT
--     m.id_menu,
--     m.name,
--     m.url,
--     m.icon,
--     m.feature_code,
--     m.status
-- FROM public.menu m
-- WHERE m.parent_id_menu = 16
-- ORDER BY m.id_menu;

-- Expected: Should show menus 17, 18, 32 (or 41), 33
