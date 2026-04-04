-- ============================================
-- Script: Granularize Menu Feature Codes
-- Date: 2025-12-19
-- Purpose: Assign unique feature_code to each leaf menu for granular permission control
-- Strategy: Reset generic feature codes, create new granular ones, assign to leaf menus
-- ============================================

-- ============================================
-- STEP 1: Create new granular feature codes
-- ============================================

-- ============================================
-- DRAFT MANAGEMENT
-- ============================================
INSERT INTO public.feature_permission 
    (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES 
    (
        'DRAFT_MANAGEMENT',
        'Gestión de Borradores',
        'Permite gestionar borradores de facturas y documentos',
        'INVOICING',
        ARRAY[1, 2, 3], -- Super Admin, Member, Admin
        ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    )
ON CONFLICT (feature_code) DO NOTHING;

-- ============================================
-- GLOBAL ADMINISTRATION FEATURES
-- ============================================
INSERT INTO public.feature_permission 
    (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES 
    (
        'GLOBAL_ESTABLISHMENT_MANAGEMENT',
        'Gestión Global de Sucursales',
        'Permite gestionar sucursales de todas las compañías (solo super_admin)',
        'ADMINISTRATION',
        ARRAY[1], -- Solo Super Admin
        ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    ),
    (
        'GLOBAL_INVOICE_POINT_MANAGEMENT',
        'Gestión Global de Puntos de Emisión',
        'Permite gestionar puntos de emisión de todas las compañías (solo super_admin)',
        'ADMINISTRATION',
        ARRAY[1], -- Solo Super Admin
        ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    )
ON CONFLICT (feature_code) DO NOTHING;

-- ============================================
-- GRANULAR REPORT FEATURES
-- ============================================
INSERT INTO public.feature_permission 
    (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES 
    -- Reportes Operativos
    (
        'REPORT_SALES_BY_PERIOD',
        'Reporte: Ventas por Periodo',
        'Permite ver reportes de ventas filtrados por periodo',
        'REPORTS',
        ARRAY[1, 3], -- Super Admin, Admin
        ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    ),
    (
        'REPORT_SALES_BY_CLIENT',
        'Reporte: Ventas por Cliente',
        'Permite ver reportes de ventas filtrados por cliente',
        'REPORTS',
        ARRAY[1, 3],
        ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    ),
    (
        'REPORT_SALES_BY_PRODUCT',
        'Reporte: Ventas por Producto',
        'Permite ver reportes de ventas filtrados por producto',
        'REPORTS',
        ARRAY[1, 3],
        ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    ),
    (
        'REPORT_CREDIT_NOTES',
        'Reporte: Notas de Crédito',
        'Permite ver reportes de notas de crédito',
        'REPORTS',
        ARRAY[1, 3],
        ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    ),
    (
        'REPORT_PENDING_INVOICES',
        'Reporte: Facturas Pendientes',
        'Permite ver reportes de facturas pendientes de envío',
        'REPORTS',
        ARRAY[1, 3],
        ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    ),
    -- Reportes Avanzados/Comerciales
    (
        'REPORT_SALES_DASHBOARD',
        'Dashboard de Ventas',
        'Permite acceder al dashboard avanzado de ventas',
        'REPORTS',
        ARRAY[1, 3],
        ARRAY['PROFESSIONAL', 'ENTERPRISE'],
        true
    ),
    (
        'REPORT_BRANCH_COMPARISON',
        'Comparativo por Sucursal',
        'Permite ver comparativos de ventas por sucursal/punto de emisión',
        'REPORTS',
        ARRAY[1, 3],
        ARRAY['PROFESSIONAL', 'ENTERPRISE'],
        true
    ),
    (
        'REPORT_PAYMENT_METHODS',
        'Reporte: Formas de Pago',
        'Permite ver reportes de formas de pago',
        'REPORTS',
        ARRAY[1, 3],
        ARRAY['BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    )
ON CONFLICT (feature_code) DO NOTHING;

-- ============================================
-- GRANULAR ADMINISTRATION FEATURES
-- ============================================
INSERT INTO public.feature_permission 
    (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES 
    (
        'INVOICE_POINT_MANAGEMENT',
        'Gestión de Puntos de Emisión',
        'Permite gestionar puntos de emisión de la compañía',
        'ADMINISTRATION',
        ARRAY[1, 3], -- Super Admin, Admin
        ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    ),
    (
        'COMPANY_USER_MANAGEMENT',
        'Gestión de Usuarios por Compañía',
        'Permite gestionar usuarios asociados a una compañía específica',
        'ADMINISTRATION',
        ARRAY[1, 3],
        ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    )
ON CONFLICT (feature_code) DO NOTHING;

-- ============================================
-- STEP 2: Clean feature_code from parent/intermediate menus
-- ============================================

-- Remove feature_code from any menu that has submenus
UPDATE public.menu
SET feature_code = NULL
WHERE id_menu IN (
    SELECT DISTINCT parent_id_menu
    FROM public.menu
    WHERE parent_id_menu IS NOT NULL
)
AND status = true;

-- ============================================
-- STEP 3: Assign unique feature codes to leaf menus
-- ============================================

-- ============================================
-- DRAFT MANAGEMENT (ID: 12)
-- ============================================
UPDATE public.menu
SET feature_code = 'DRAFT_MANAGEMENT'
WHERE id_menu = 12
  AND url = '/drafts'
  AND status = true;

-- ============================================
-- GLOBAL ADMINISTRATION (IDs: 17, 18)
-- ============================================
UPDATE public.menu
SET feature_code = 'GLOBAL_ESTABLISHMENT_MANAGEMENT'
WHERE id_menu = 17
  AND url = '/admin/establishments'
  AND status = true;

UPDATE public.menu
SET feature_code = 'GLOBAL_INVOICE_POINT_MANAGEMENT'
WHERE id_menu = 18
  AND url = '/admin/invoice-points'
  AND status = true;

-- ============================================
-- GRANULAR REPORTS - OPERATIONAL (IDs: 22, 23, 24, 25, 27)
-- ============================================
UPDATE public.menu
SET feature_code = 'REPORT_SALES_BY_PERIOD'
WHERE id_menu = 22
  AND url = 'sales-by-period'
  AND status = true;

UPDATE public.menu
SET feature_code = 'REPORT_SALES_BY_CLIENT'
WHERE id_menu = 23
  AND url = 'sales-by-client'
  AND status = true;

UPDATE public.menu
SET feature_code = 'REPORT_SALES_BY_PRODUCT'
WHERE id_menu = 24
  AND url = 'sales-by-product'
  AND status = true;

UPDATE public.menu
SET feature_code = 'REPORT_CREDIT_NOTES'
WHERE id_menu = 25
  AND url = 'credit-notes'
  AND status = true;

UPDATE public.menu
SET feature_code = 'REPORT_PENDING_INVOICES'
WHERE id_menu = 27
  AND url = 'pending-invoices'
  AND status = true;

-- ============================================
-- GRANULAR REPORTS - ADVANCED/COMMERCIAL (IDs: 29, 30, 31)
-- ============================================
UPDATE public.menu
SET feature_code = 'REPORT_SALES_DASHBOARD'
WHERE id_menu = 29
  AND url = 'sales-dashboard'
  AND status = true;

UPDATE public.menu
SET feature_code = 'REPORT_BRANCH_COMPARISON'
WHERE id_menu = 30
  AND url = 'branch-comparison'
  AND status = true;

UPDATE public.menu
SET feature_code = 'REPORT_PAYMENT_METHODS'
WHERE id_menu = 31
  AND url = 'payment-methods'
  AND status = true;

-- ============================================
-- GRANULAR ADMINISTRATION (IDs: 7, 15)
-- ============================================
UPDATE public.menu
SET feature_code = 'COMPANY_USER_MANAGEMENT'
WHERE id_menu = 7
  AND url = '/company-users'
  AND status = true;

UPDATE public.menu
SET feature_code = 'INVOICE_POINT_MANAGEMENT'
WHERE id_menu = 15
  AND url = '/invoice-points'
  AND status = true;

-- ============================================
-- STEP 4: Deactivate generic feature codes (for manual reassignment via UI)
-- ============================================

-- Deactivate REPORT_OPERATIONAL, REPORT_ADVANCED, REPORT_SALES
-- These will be manually reassigned via the UI
UPDATE public.feature_permission
SET is_active = false
WHERE feature_code IN ('REPORT_OPERATIONAL', 'REPORT_ADVANCED', 'REPORT_SALES')
  AND is_active = true;

-- ============================================
-- STEP 5: Remove generic feature codes from menu_role assignments
-- ============================================

-- Note: This step removes role assignments for generic features
-- These will need to be reassigned manually via the UI with granular features

-- First, get all menu_role entries that reference menus with generic feature codes
-- Then remove those assignments (they'll be reassigned with granular features)

-- This is handled by the fact that we're updating the menu.feature_code
-- The menu_role table references menu.id_menu, not feature_code directly
-- So existing menu_role entries remain, but now point to menus with granular feature codes

-- ============================================
-- VERIFICATION QUERIES (commented - run manually)
-- ============================================

-- Verify all new feature codes were created
-- SELECT feature_code, feature_name, module, is_active
-- FROM public.feature_permission
-- WHERE feature_code IN (
--     'DRAFT_MANAGEMENT',
--     'GLOBAL_ESTABLISHMENT_MANAGEMENT',
--     'GLOBAL_INVOICE_POINT_MANAGEMENT',
--     'REPORT_SALES_BY_PERIOD',
--     'REPORT_SALES_BY_CLIENT',
--     'REPORT_SALES_BY_PRODUCT',
--     'REPORT_CREDIT_NOTES',
--     'REPORT_PENDING_INVOICES',
--     'REPORT_SALES_DASHBOARD',
--     'REPORT_BRANCH_COMPARISON',
--     'REPORT_PAYMENT_METHODS',
--     'INVOICE_POINT_MANAGEMENT',
--     'COMPANY_USER_MANAGEMENT'
-- )
-- ORDER BY module, feature_code;

-- Verify no duplicate feature codes in menus
-- SELECT feature_code, COUNT(*) as count, STRING_AGG(id_menu::text, ', ') as menu_ids
-- FROM public.menu
-- WHERE feature_code IS NOT NULL
--   AND status = true
-- GROUP BY feature_code
-- HAVING COUNT(*) > 1;

-- Expected: Should return 0 rows (no duplicates)

-- Verify all leaf menus have feature_code
-- SELECT m.id_menu, m.name, m.url, m.feature_code
-- FROM public.menu m
-- WHERE m.status = true
--   AND NOT EXISTS (
--       SELECT 1 FROM public.menu m2 WHERE m2.parent_id_menu = m.id_menu
--   )
--   AND m.feature_code IS NULL
-- ORDER BY m.id_menu;

-- Expected: Should return 0 rows (all leaf menus have feature_code)

-- Verify no parent/intermediate menus have feature_code
-- SELECT m.id_menu, m.name, m.feature_code,
--        (SELECT COUNT(*) FROM public.menu m2 WHERE m2.parent_id_menu = m.id_menu) as submenus_count
-- FROM public.menu m
-- WHERE m.status = true
--   AND EXISTS (
--       SELECT 1 FROM public.menu m2 WHERE m2.parent_id_menu = m.id_menu
--   )
--   AND m.feature_code IS NOT NULL
-- ORDER BY m.id_menu;

-- Expected: Should return 0 rows (no parent menus have feature_code)

-- Verify generic feature codes are deactivated
-- SELECT feature_code, feature_name, is_active
-- FROM public.feature_permission
-- WHERE feature_code IN ('REPORT_OPERATIONAL', 'REPORT_ADVANCED', 'REPORT_SALES');

-- Expected: is_active = false for all three

-- List all menus with their feature codes (final state)
-- SELECT 
--     m.id_menu,
--     m.name,
--     m.url,
--     m.feature_code,
--     (SELECT name FROM public.menu WHERE id_menu = m.parent_id_menu) as parent_name,
--     (SELECT COUNT(*) FROM public.menu m2 WHERE m2.parent_id_menu = m.id_menu) as submenus_count
-- FROM public.menu m
-- WHERE m.status = true
-- ORDER BY m.parent_id_menu NULLS FIRST, m.id_menu;
