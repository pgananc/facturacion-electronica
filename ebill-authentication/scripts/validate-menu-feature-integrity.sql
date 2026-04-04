-- ============================================
-- Script: Validate Menu Feature Code Integrity
-- Date: 2025-12-17
-- Purpose: Validate data integrity after adding feature_code to menu table
-- Run this script AFTER migration V2025121701 to ensure everything is correct
-- ============================================

\echo '============================================'
\echo 'MENU FEATURE CODE INTEGRITY VALIDATION'
\echo '============================================'
\echo ''

-- ============================================
-- 1. Check for orphaned feature codes
-- ============================================
\echo '1. Checking for orphaned feature codes (menu items referencing non-existent features)...'
SELECT 
    m.id_menu,
    m.name,
    m.url,
    m.feature_code AS invalid_feature_code
FROM public.menu m
LEFT JOIN public.feature_permission fp ON m.feature_code = fp.feature_code
WHERE m.feature_code IS NOT NULL
  AND fp.feature_code IS NULL;

\echo '   ✓ Expected: 0 rows (all feature codes should exist in feature_permission table)'
\echo ''

-- ============================================
-- 2. Count menus with feature codes
-- ============================================
\echo '2. Counting menus with feature codes assigned...'
SELECT 
    COUNT(*) as menus_with_feature_code,
    COUNT(DISTINCT feature_code) as unique_feature_codes
FROM public.menu 
WHERE feature_code IS NOT NULL 
  AND status = true;

\echo '   ℹ Expected: ~15-20 menus with feature codes'
\echo ''

-- ============================================
-- 3. Count menus without feature codes
-- ============================================
\echo '3. Counting menus WITHOUT feature codes (should be parent menus)...'
SELECT 
    COUNT(*) as menus_without_feature_code
FROM public.menu 
WHERE feature_code IS NULL 
  AND status = true;

\echo '   ℹ Expected: ~8-10 parent menus without feature codes'
\echo ''

-- ============================================
-- 4. List parent menus without feature codes
-- ============================================
\echo '4. Listing parent menus (containers) without feature codes...'
SELECT 
    m.id_menu,
    m.name,
    m.url,
    (SELECT COUNT(*) FROM menu WHERE parent_id_menu = m.id_menu) as children_count
FROM public.menu m
WHERE m.feature_code IS NULL 
  AND m.status = true
  AND m.parent_id_menu IS NULL
ORDER BY m.id_menu;

\echo '   ℹ These should be top-level container menus (Cliente, Producto, Compañía, Usuario, etc.)'
\echo ''

-- ============================================
-- 5. Show feature code usage summary
-- ============================================
\echo '5. Feature code usage summary...'
SELECT 
    fp.feature_code,
    fp.feature_name,
    fp.module,
    COUNT(m.id_menu) as menu_count
FROM public.feature_permission fp
LEFT JOIN public.menu m ON m.feature_code = fp.feature_code AND m.status = true
WHERE fp.is_active = true
GROUP BY fp.feature_code, fp.feature_name, fp.module
ORDER BY COUNT(m.id_menu) DESC, fp.module, fp.feature_code;

\echo '   ℹ Shows which features are used by menus'
\echo ''

-- ============================================
-- 6. Check for menus with URL but no feature code
-- ============================================
\echo '6. Checking for ACTION menus (with URL) that lack feature codes...'
SELECT 
    m.id_menu,
    m.name,
    m.url,
    m.parent_id_menu,
    p.name as parent_name
FROM public.menu m
LEFT JOIN public.menu p ON p.id_menu = m.parent_id_menu
WHERE m.url IS NOT NULL 
  AND m.url != 'null'
  AND m.feature_code IS NULL
  AND m.status = true
ORDER BY m.id_menu;

\echo '   ⚠ Expected: 0 rows (all action menus should have feature codes)'
\echo '   ℹ If any rows appear, these menus need feature codes assigned!'
\echo ''

-- ============================================
-- 7. Show complete menu hierarchy with feature codes
-- ============================================
\echo '7. Complete menu hierarchy with feature codes...'
WITH RECURSIVE menu_tree AS (
    SELECT 
        id_menu, 
        name, 
        url, 
        feature_code, 
        parent_id_menu, 
        0 as level,
        CAST(id_menu AS TEXT) as path,
        LPAD(CAST(id_menu AS TEXT), 3, '0') as sort_path
    FROM menu 
    WHERE parent_id_menu IS NULL AND status = true
    
    UNION ALL
    
    SELECT 
        m.id_menu, 
        m.name, 
        m.url, 
        m.feature_code, 
        m.parent_id_menu, 
        mt.level + 1,
        mt.path || '->' || CAST(m.id_menu AS TEXT),
        mt.sort_path || '.' || LPAD(CAST(m.id_menu AS TEXT), 3, '0')
    FROM menu m
    INNER JOIN menu_tree mt ON m.parent_id_menu = mt.id_menu
    WHERE m.status = true
)
SELECT 
    REPEAT('  ', level) || name as menu_hierarchy,
    url,
    feature_code,
    CASE 
        WHEN url IS NOT NULL AND url != 'null' AND feature_code IS NULL THEN '⚠ MISSING'
        WHEN feature_code IS NOT NULL THEN '✓ OK'
        ELSE '(container)'
    END as status
FROM menu_tree
ORDER BY sort_path;

\echo ''
\echo '   ℹ Hierarchical view of all menus with their feature codes'
\echo ''

-- ============================================
-- 8. Verify FK constraint exists
-- ============================================
\echo '8. Verifying foreign key constraint exists...'
SELECT 
    tc.constraint_name,
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_name = 'menu'
    AND kcu.column_name = 'feature_code';

\echo '   ✓ Expected: 1 row showing FK constraint fk_menu_feature_code'
\echo ''

-- ============================================
-- 9. Check for inactive features used by active menus
-- ============================================
\echo '9. Checking for inactive features used by active menus...'
SELECT 
    m.id_menu,
    m.name,
    m.url,
    fp.feature_code,
    fp.feature_name,
    fp.is_active
FROM public.menu m
INNER JOIN public.feature_permission fp ON m.feature_code = fp.feature_code
WHERE m.status = true
  AND fp.is_active = false;

\echo '   ✓ Expected: 0 rows (active menus should only use active features)'
\echo ''

-- ============================================
-- SUMMARY
-- ============================================
\echo '============================================'
\echo 'VALIDATION SUMMARY'
\echo '============================================'
\echo ''
\echo 'If all checks pass:'
\echo '  ✓ No orphaned feature codes'
\echo '  ✓ All action menus have feature codes'
\echo '  ✓ FK constraint exists and is valid'
\echo '  ✓ No inactive features in use'
\echo ''
\echo 'Next steps:'
\echo '  1. Review menu hierarchy (section 7)'
\echo '  2. Test menu API endpoint: GET /api/menu/{username}'
\echo '  3. Verify frontend receives featureCode in response'
\echo '  4. Test permission validation in frontend'
\echo ''
\echo '============================================'
