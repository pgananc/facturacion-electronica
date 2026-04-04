-- ============================================
-- Script: Validate Granular Feature Codes Migration
-- Date: 2025-12-19
-- Purpose: Comprehensive validation after granularizing menu feature codes
-- ============================================

-- ============================================
-- 1. Verify all new feature codes exist and are active
-- ============================================
SELECT 
    '1. Nuevos Feature Codes Creados' as validation,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) = 13 THEN '✅ CORRECTO - 13 nuevos feature codes creados'
        ELSE '❌ ERROR - Faltan feature codes'
    END as status
FROM public.feature_permission
WHERE feature_code IN (
    'DRAFT_MANAGEMENT',
    'GLOBAL_ESTABLISHMENT_MANAGEMENT',
    'GLOBAL_INVOICE_POINT_MANAGEMENT',
    'REPORT_SALES_BY_PERIOD',
    'REPORT_SALES_BY_CLIENT',
    'REPORT_SALES_BY_PRODUCT',
    'REPORT_CREDIT_NOTES',
    'REPORT_PENDING_INVOICES',
    'REPORT_SALES_DASHBOARD',
    'REPORT_BRANCH_COMPARISON',
    'REPORT_PAYMENT_METHODS',
    'INVOICE_POINT_MANAGEMENT',
    'COMPANY_USER_MANAGEMENT'
)
AND is_active = true;

-- ============================================
-- 2. Verify no duplicate feature codes in menus
-- ============================================
SELECT 
    '2. Feature Codes Duplicados en Menús' as validation,
    COUNT(*) as duplicate_count,
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ CORRECTO - No hay duplicados'
        ELSE '❌ ERROR - Hay feature codes duplicados'
    END as status
FROM (
    SELECT feature_code, COUNT(*) as count
    FROM public.menu
    WHERE feature_code IS NOT NULL
      AND status = true
    GROUP BY feature_code
    HAVING COUNT(*) > 1
) duplicates;

-- ============================================
-- 3. Verify all leaf menus have feature_code
-- ============================================
SELECT 
    '3. Menús Hoja sin Feature Code' as validation,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ CORRECTO - Todos los menús hoja tienen feature_code'
        ELSE '❌ ERROR - Hay menús hoja sin feature_code'
    END as status
FROM public.menu m
WHERE m.status = true
  AND NOT EXISTS (
      SELECT 1 FROM public.menu m2 WHERE m2.parent_id_menu = m.id_menu
  )
  AND m.feature_code IS NULL;

-- ============================================
-- 4. Verify no parent/intermediate menus have feature_code
-- ============================================
SELECT 
    '4. Menús Padre/Intermedios con Feature Code' as validation,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ CORRECTO - Ningún menú padre tiene feature_code'
        ELSE '❌ ERROR - Hay menús padre con feature_code'
    END as status
FROM public.menu m
WHERE m.status = true
  AND EXISTS (
      SELECT 1 FROM public.menu m2 WHERE m2.parent_id_menu = m.id_menu
  )
  AND m.feature_code IS NOT NULL;

-- ============================================
-- 5. Verify generic feature codes are deactivated
-- ============================================
SELECT 
    '5. Feature Codes Genéricos Desactivados' as validation,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) = 3 THEN '✅ CORRECTO - 3 feature codes genéricos desactivados'
        ELSE '⚠️ ADVERTENCIA - No todos los genéricos están desactivados'
    END as status
FROM public.feature_permission
WHERE feature_code IN ('REPORT_OPERATIONAL', 'REPORT_ADVANCED', 'REPORT_SALES')
  AND is_active = false;

-- ============================================
-- 6. Detail: All leaf menus with their feature codes
-- ============================================
SELECT 
    '6. Detalle de Menús Hoja' as validation,
    m.id_menu,
    m.name,
    m.url,
    m.feature_code,
    (SELECT name FROM public.menu WHERE id_menu = m.parent_id_menu) as parent_name
FROM public.menu m
WHERE m.status = true
  AND NOT EXISTS (
      SELECT 1 FROM public.menu m2 WHERE m2.parent_id_menu = m.id_menu
  )
ORDER BY m.parent_id_menu NULLS FIRST, m.id_menu;

-- ============================================
-- 7. Detail: All parent/intermediate menus (should have NULL feature_code)
-- ============================================
SELECT 
    '7. Detalle de Menús Padre/Intermedios' as validation,
    m.id_menu,
    m.name,
    m.feature_code,
    (SELECT COUNT(*) FROM public.menu m2 WHERE m2.parent_id_menu = m.id_menu) as submenus_count
FROM public.menu m
WHERE m.status = true
  AND EXISTS (
      SELECT 1 FROM public.menu m2 WHERE m2.parent_id_menu = m.id_menu
  )
ORDER BY m.id_menu;

-- ============================================
-- 8. Verify specific menu assignments
-- ============================================
SELECT 
    '8. Asignaciones Específicas de Menús' as validation,
    m.id_menu,
    m.name,
    m.url,
    m.feature_code,
    CASE 
        WHEN m.id_menu = 12 AND m.feature_code = 'DRAFT_MANAGEMENT' THEN '✅ CORRECTO'
        WHEN m.id_menu = 17 AND m.feature_code = 'GLOBAL_ESTABLISHMENT_MANAGEMENT' THEN '✅ CORRECTO'
        WHEN m.id_menu = 18 AND m.feature_code = 'GLOBAL_INVOICE_POINT_MANAGEMENT' THEN '✅ CORRECTO'
        WHEN m.id_menu = 22 AND m.feature_code = 'REPORT_SALES_BY_PERIOD' THEN '✅ CORRECTO'
        WHEN m.id_menu = 23 AND m.feature_code = 'REPORT_SALES_BY_CLIENT' THEN '✅ CORRECTO'
        WHEN m.id_menu = 24 AND m.feature_code = 'REPORT_SALES_BY_PRODUCT' THEN '✅ CORRECTO'
        WHEN m.id_menu = 25 AND m.feature_code = 'REPORT_CREDIT_NOTES' THEN '✅ CORRECTO'
        WHEN m.id_menu = 27 AND m.feature_code = 'REPORT_PENDING_INVOICES' THEN '✅ CORRECTO'
        WHEN m.id_menu = 29 AND m.feature_code = 'REPORT_SALES_DASHBOARD' THEN '✅ CORRECTO'
        WHEN m.id_menu = 30 AND m.feature_code = 'REPORT_BRANCH_COMPARISON' THEN '✅ CORRECTO'
        WHEN m.id_menu = 31 AND m.feature_code = 'REPORT_PAYMENT_METHODS' THEN '✅ CORRECTO'
        WHEN m.id_menu = 7 AND m.feature_code = 'COMPANY_USER_MANAGEMENT' THEN '✅ CORRECTO'
        WHEN m.id_menu = 15 AND m.feature_code = 'INVOICE_POINT_MANAGEMENT' THEN '✅ CORRECTO'
        ELSE '❌ ERROR'
    END as status
FROM public.menu m
WHERE m.id_menu IN (12, 17, 18, 22, 23, 24, 25, 27, 29, 30, 31, 7, 15)
  AND m.status = true
ORDER BY m.id_menu;

-- ============================================
-- 9. Count total feature codes (active)
-- ============================================
SELECT 
    '9. Total Feature Codes Activos' as validation,
    COUNT(*) as total_active,
    COUNT(CASE WHEN module = 'REPORTS' THEN 1 END) as reports_count,
    COUNT(CASE WHEN module = 'ADMINISTRATION' THEN 1 END) as admin_count,
    COUNT(CASE WHEN module = 'INVOICING' THEN 1 END) as invoicing_count
FROM public.feature_permission
WHERE is_active = true;

-- ============================================
-- 10. Verify feature codes that should remain unchanged
-- ============================================
SELECT 
    '10. Feature Codes Sin Cambios' as validation,
    m.id_menu,
    m.name,
    m.feature_code,
    CASE 
        WHEN m.id_menu = 6 AND m.feature_code = 'INVOICE_CREATE' THEN '✅ CORRECTO'
        WHEN m.id_menu = 19 AND m.feature_code = 'CREDIT_NOTE_CREATE' THEN '✅ CORRECTO'
        WHEN m.id_menu = 8 AND m.feature_code = 'CLIENT_CREATE' THEN '✅ CORRECTO'
        WHEN m.id_menu = 11 AND m.feature_code = 'USER_MANAGEMENT' THEN '✅ CORRECTO'
        WHEN m.id_menu = 10 AND m.feature_code = 'COMPANY_SETTINGS' THEN '✅ CORRECTO'
        WHEN m.id_menu = 14 AND m.feature_code = 'ESTABLISHMENT_MANAGEMENT' THEN '✅ CORRECTO'
        WHEN m.id_menu = 9 AND m.feature_code = 'PRODUCT_MANAGEMENT' THEN '✅ CORRECTO'
        WHEN m.id_menu = 32 AND m.feature_code = 'ROLE_PERMISSION_MANAGEMENT' THEN '✅ CORRECTO'
        WHEN m.id_menu = 33 AND m.feature_code = 'SUBSCRIPTION_MANAGEMENT' THEN '✅ CORRECTO'
        ELSE '⚠️ VERIFICAR'
    END as status
FROM public.menu m
WHERE m.id_menu IN (6, 19, 8, 11, 10, 14, 9, 32, 33)
  AND m.status = true
ORDER BY m.id_menu;
