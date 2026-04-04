-- ============================================
-- Script: Validate Admin Role Permissions
-- Date: 2025-12-17
-- Purpose: Verify that Admin role (id=3) has correct menu and feature permissions
-- Run this after migrations V2025121702 and V2025121703
-- ============================================

\echo '============================================'
\echo 'ADMIN ROLE (id=3) PERMISSIONS VALIDATION'
\echo '============================================'
\echo ''

-- ============================================
-- 1. VERIFY MENU ASSIGNMENTS
-- ============================================

\echo '1. Counting menus assigned to Admin role...'
SELECT 
    COUNT(*) as total_menus_assigned,
    COUNT(DISTINCT CASE WHEN m.parent_id_menu IS NULL THEN m.id_menu END) as parent_menus,
    COUNT(DISTINCT CASE WHEN m.parent_id_menu IS NOT NULL THEN m.id_menu END) as submenus
FROM public.menu_role mr
INNER JOIN public.menu m ON m.id_menu = mr.id_menu
WHERE mr.id_role = 3 
  AND mr.status = true
  AND m.status = true;

\echo '   ✓ Expected: 28 total (7 parents + 21 submenus)'
\echo ''

-- ============================================
-- 2. LIST ALL MENUS FOR ADMIN ROLE
-- ============================================

\echo '2. Listing all menus accessible by Admin role...'
WITH RECURSIVE menu_tree AS (
    SELECT 
        m.id_menu, 
        m.name, 
        m.url,
        m.feature_code,
        m.parent_id_menu, 
        0 as level,
        CAST(m.id_menu AS TEXT) as path
    FROM public.menu m
    INNER JOIN public.menu_role mr ON mr.id_menu = m.id_menu
    WHERE mr.id_role = 3 
      AND mr.status = true
      AND m.status = true
      AND m.parent_id_menu IS NULL
    
    UNION ALL
    
    SELECT 
        m.id_menu, 
        m.name, 
        m.url,
        m.feature_code,
        m.parent_id_menu, 
        mt.level + 1,
        mt.path || '->' || CAST(m.id_menu AS TEXT)
    FROM public.menu m
    INNER JOIN public.menu_role mr ON mr.id_menu = m.id_menu
    INNER JOIN menu_tree mt ON m.parent_id_menu = mt.id_menu
    WHERE mr.id_role = 3 
      AND mr.status = true
      AND m.status = true
)
SELECT 
    id_menu,
    REPEAT('  ', level) || name as menu_hierarchy,
    url,
    feature_code,
    CASE 
        WHEN level = 0 THEN '📁 Parent'
        WHEN url IS NULL OR url = 'null' THEN '📂 Container'
        ELSE '📄 Action'
    END as type
FROM menu_tree
ORDER BY path;

\echo ''
\echo '   ℹ Should show 28 menus in hierarchical structure'
\echo ''

-- ============================================
-- 3. VERIFY EXCLUDED MENUS
-- ============================================

\echo '3. Verifying Admin does NOT have access to restricted menus...'
SELECT 
    m.id_menu,
    m.name,
    m.url,
    'SHOULD NOT APPEAR!' as warning
FROM public.menu_role mr
INNER JOIN public.menu m ON m.id_menu = mr.id_menu
WHERE mr.id_role = 3 
  AND mr.status = true
  AND m.id_menu IN (16, 17, 18); -- Administración Global menus

\echo '   ✓ Expected: 0 rows (Admin should NOT see Administración Global)'
\echo ''

-- ============================================
-- 4. VERIFY FEATURE PERMISSIONS
-- ============================================

\echo '4. Counting features accessible by Admin role...'
SELECT 
    COUNT(*) as total_features_assigned,
    COUNT(DISTINCT module) as modules_covered
FROM public.feature_permission
WHERE 3 = ANY(allowed_roles) 
  AND is_active = true;

\echo '   ✓ Expected: 14 features across 5 modules'
\echo ''

-- ============================================
-- 5. LIST ALL FEATURES FOR ADMIN ROLE
-- ============================================

\echo '5. Listing all features accessible by Admin role...'
SELECT 
    fp.module,
    fp.feature_code,
    fp.feature_name,
    array_to_string(fp.required_plans, ', ') as required_plans,
    CASE 
        WHEN 1 = ANY(fp.allowed_roles) THEN '✓' ELSE '✗' 
    END as super_admin,
    CASE 
        WHEN 2 = ANY(fp.allowed_roles) THEN '✓' ELSE '✗' 
    END as member,
    CASE 
        WHEN 3 = ANY(fp.allowed_roles) THEN '✓' ELSE '✗' 
    END as admin
FROM public.feature_permission fp
WHERE 3 = ANY(fp.allowed_roles) 
  AND fp.is_active = true
ORDER BY fp.module, fp.feature_code;

\echo ''
\echo '   ℹ Should show 14 features with Admin column marked ✓'
\echo ''

-- ============================================
-- 6. VERIFY EXCLUDED FEATURES
-- ============================================

\echo '6. Verifying Admin does NOT have access to restricted features...'
SELECT 
    feature_code,
    feature_name,
    module,
    allowed_roles,
    'SHOULD NOT INCLUDE ROLE 3!' as warning
FROM public.feature_permission
WHERE feature_code IN ('API_ACCESS', 'WEBHOOK_CONFIG')
  AND 3 = ANY(allowed_roles);

\echo '   ✓ Expected: 0 rows (Admin should NOT have API_ACCESS or WEBHOOK_CONFIG)'
\echo ''

-- ============================================
-- 7. COMPARE ROLES: Super Admin vs Admin
-- ============================================

\echo '7. Comparing Super Admin vs Admin permissions...'

\echo ''
\echo '   MENU ACCESS COMPARISON:'
SELECT 
    'Super Admin' as role,
    COUNT(DISTINCT mr.id_menu) as menu_count
FROM public.menu_role mr
WHERE mr.id_role = 1 AND mr.status = true

UNION ALL

SELECT 
    'Admin' as role,
    COUNT(DISTINCT mr.id_menu) as menu_count
FROM public.menu_role mr
WHERE mr.id_role = 3 AND mr.status = true;

\echo ''
\echo '   FEATURE ACCESS COMPARISON:'
SELECT 
    'Super Admin' as role,
    COUNT(*) as feature_count
FROM public.feature_permission
WHERE 1 = ANY(allowed_roles) AND is_active = true

UNION ALL

SELECT 
    'Admin' as role,
    COUNT(*) as feature_count
FROM public.feature_permission
WHERE 3 = ANY(allowed_roles) AND is_active = true;

\echo ''

-- ============================================
-- 8. MENUS WITHOUT FEATURE CODES (Admin accessible)
-- ============================================

\echo '8. Checking action menus (with URL) that lack feature codes...'
SELECT 
    m.id_menu,
    m.name,
    m.url,
    m.feature_code,
    CASE WHEN m.feature_code IS NULL THEN '⚠ MISSING FEATURE CODE' ELSE 'OK' END as status
FROM public.menu m
INNER JOIN public.menu_role mr ON mr.id_menu = m.id_menu
WHERE mr.id_role = 3
  AND mr.status = true
  AND m.status = true
  AND m.url IS NOT NULL
  AND m.url != 'null'
  AND m.feature_code IS NULL;

\echo '   ⚠ Expected: 0 rows (all action menus should have feature codes)'
\echo ''

-- ============================================
-- 9. FEATURE PERMISSIONS SUMMARY BY MODULE
-- ============================================

\echo '9. Feature permissions summary by module...'
SELECT 
    fp.module,
    COUNT(*) as total_features,
    SUM(CASE WHEN 1 = ANY(fp.allowed_roles) THEN 1 ELSE 0 END) as super_admin_access,
    SUM(CASE WHEN 2 = ANY(fp.allowed_roles) THEN 1 ELSE 0 END) as member_access,
    SUM(CASE WHEN 3 = ANY(fp.allowed_roles) THEN 1 ELSE 0 END) as admin_access
FROM public.feature_permission fp
WHERE fp.is_active = true
GROUP BY fp.module
ORDER BY fp.module;

\echo ''
\echo '   ℹ Shows feature distribution across roles and modules'
\echo ''

-- ============================================
-- 10. ADMIN ROLE CAPABILITIES SUMMARY
-- ============================================

\echo '10. Admin role capabilities summary...'
\echo ''
\echo '    ADMIN CAN:'
\echo '    ✅ Manage clients (create, import)'
\echo '    ✅ Manage products'
\echo '    ✅ Manage company settings'
\echo '    ✅ Manage users within their company'
\echo '    ✅ Manage establishments and invoice points'
\echo '    ✅ Create and void invoices'
\echo '    ✅ Create and void credit notes'
\echo '    ✅ Access all reports (operational, sales, advanced)'
\echo ''
\echo '    ADMIN CANNOT:'
\echo '    ❌ Access Global Administration menu'
\echo '    ❌ Manage other companies'
\echo '    ❌ Access API endpoints'
\echo '    ❌ Configure webhooks'
\echo ''

-- ============================================
-- 11. DETAILED BREAKDOWN: Expected vs Actual
-- ============================================

\echo '11. Validation breakdown...'
\echo ''

-- Menu count validation
WITH menu_count AS (
    SELECT COUNT(*) as actual
    FROM public.menu_role mr
    WHERE mr.id_role = 3 AND mr.status = true
)
SELECT 
    'Menu Assignments' as validation_item,
    28 as expected,
    (SELECT actual FROM menu_count) as actual,
    CASE 
        WHEN (SELECT actual FROM menu_count) = 28 THEN '✅ PASS'
        ELSE '❌ FAIL'
    END as status;

-- Feature count validation
WITH feature_count AS (
    SELECT COUNT(*) as actual
    FROM public.feature_permission
    WHERE 3 = ANY(allowed_roles) AND is_active = true
)
SELECT 
    'Feature Assignments' as validation_item,
    14 as expected,
    (SELECT actual FROM feature_count) as actual,
    CASE 
        WHEN (SELECT actual FROM feature_count) = 14 THEN '✅ PASS'
        ELSE '❌ FAIL'
    END as status;

-- Excluded menus validation
WITH excluded_menus AS (
    SELECT COUNT(*) as actual
    FROM public.menu_role mr
    WHERE mr.id_role = 3 
      AND mr.status = true
      AND mr.id_menu IN (16, 17, 18)
)
SELECT 
    'Excluded Menus (16,17,18)' as validation_item,
    0 as expected,
    (SELECT actual FROM excluded_menus) as actual,
    CASE 
        WHEN (SELECT actual FROM excluded_menus) = 0 THEN '✅ PASS'
        ELSE '❌ FAIL - Admin has access to restricted menus!'
    END as status;

-- Excluded features validation
WITH excluded_features AS (
    SELECT COUNT(*) as actual
    FROM public.feature_permission
    WHERE feature_code IN ('API_ACCESS', 'WEBHOOK_CONFIG')
      AND 3 = ANY(allowed_roles)
)
SELECT 
    'Excluded Features (API)' as validation_item,
    0 as expected,
    (SELECT actual FROM excluded_features) as actual,
    CASE 
        WHEN (SELECT actual FROM excluded_features) = 0 THEN '✅ PASS'
        ELSE '❌ FAIL - Admin has access to restricted features!'
    END as status;

\echo ''

-- ============================================
-- SUMMARY
-- ============================================

\echo '============================================'
\echo 'VALIDATION SUMMARY'
\echo '============================================'
\echo ''
\echo 'If all validations pass:'
\echo '  ✅ Admin role has 28 menus (7 parents + 21 submenus)'
\echo '  ✅ Admin role has 14 features'
\echo '  ✅ Admin does NOT have Global Administration access'
\echo '  ✅ Admin does NOT have API/Webhook features'
\echo ''
\echo 'Admin role is ready for use!'
\echo ''
\echo 'Next steps:'
\echo '  1. Test login with Admin user'
\echo '  2. Verify menu appears correctly in frontend'
\echo '  3. Test feature validation endpoints'
\echo '  4. Verify plan-based restrictions work'
\echo ''
\echo '============================================'
