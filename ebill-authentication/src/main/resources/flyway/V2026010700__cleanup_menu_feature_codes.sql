-- ============================================
-- Script: Cleanup Menu Feature Codes
-- Date: 2026-01-07
-- Purpose: 
--   1. Remove feature_code from parent menus (containers)
--   2. Ensure all leaf menus (children) have feature_code
--   3. Define missing feature_codes for menus that need them
-- ============================================

-- ============================================
-- STEP 1: Remove feature_code from parent menus
-- Parent menus are those that have children (parent_id_menu IS NULL or have submenus)
-- ============================================

-- Remove feature_code from all parent menus
UPDATE public.menu
SET feature_code = NULL
WHERE id_menu IN (
    -- Root level parents (no parent)
    SELECT DISTINCT m1.id_menu
    FROM public.menu m1
    WHERE m1.parent_id_menu IS NULL
    
    UNION
    
    -- Menus that have children
    SELECT DISTINCT m2.parent_id_menu
    FROM public.menu m2
    WHERE m2.parent_id_menu IS NOT NULL
)
AND status = true;

-- ============================================
-- STEP 2: Ensure all leaf menus have feature_code
-- Leaf menus are those that have a URL (action menus) and no children
-- ============================================

-- Menu ID 26: "Reporte Novedades" - This is a parent menu (has child ID 27), so it should be NULL
-- But if it has a URL, it might need a feature_code. Let's check if it's truly a leaf.
-- According to the data: ID 26 has URL '/reports/news' but also has child ID 27
-- Since it has a child, it's a parent, so feature_code should be NULL (already handled above)

-- Menu ID 21: "Reportes operativos" - Parent menu (has children 22, 23, 24, 25), feature_code should be NULL
-- Menu ID 28: "Reportes comerciales" - Parent menu (has children 29, 30, 31), feature_code should be NULL

-- All other menus with URLs should have feature_code assigned
-- Let's verify and assign missing ones:

-- Menu ID 26: "Reporte Novedades" - If it's truly a leaf (no children), assign feature_code
-- But according to data, it has child ID 27, so it's a parent and should be NULL
-- However, if it has a URL '/reports/news', we might want to assign a feature_code
-- Let's create a feature for it if it doesn't exist

-- Check if REPORT_NEWS feature exists, if not create it
INSERT INTO public.feature_permission 
    (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES 
    (
        'REPORT_NEWS',
        'Reporte: Novedades',
        'Permite ver reportes de novedades y notificaciones',
        'REPORTS',
        ARRAY[1, 3], -- Super Admin, Admin
        ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    )
ON CONFLICT (feature_code) DO NOTHING;

-- Assign REPORT_NEWS to menu ID 26 if it has a URL and is a leaf
-- But wait, ID 26 has child ID 27, so it's a parent. Let's check the actual structure.
-- According to user data: ID 26 has URL '/reports/news' and has child ID 27
-- This is a parent menu, so feature_code should be NULL

-- ============================================
-- STEP 3: Verify all leaf menus have feature_code
-- ============================================

-- List of menus that should have feature_code (leaf menus with URLs):
-- ID 22: REPORT_SALES_BY_PERIOD ✓
-- ID 23: REPORT_SALES_BY_CLIENT ✓
-- ID 24: REPORT_SALES_BY_PRODUCT ✓
-- ID 25: REPORT_CREDIT_NOTES ✓
-- ID 27: REPORT_PENDING_INVOICES ✓
-- ID 29: REPORT_SALES_DASHBOARD ✓
-- ID 30: REPORT_BRANCH_COMPARISON ✓
-- ID 31: REPORT_PAYMENT_METHODS ✓
-- ID 6: INVOICE_CREATE ✓
-- ID 8: CLIENT_CREATE ✓
-- ID 9: PRODUCT_MANAGEMENT ✓
-- ID 10: COMPANY_SETTINGS ✓
-- ID 11: USER_MANAGEMENT ✓
-- ID 12: DRAFT_MANAGEMENT ✓
-- ID 14: ESTABLISHMENT_MANAGEMENT ✓
-- ID 15: INVOICE_POINT_MANAGEMENT ✓
-- ID 17: GLOBAL_ESTABLISHMENT_MANAGEMENT ✓
-- ID 18: GLOBAL_INVOICE_POINT_MANAGEMENT ✓
-- ID 19: CREDIT_NOTE_CREATE ✓
-- ID 32: ROLE_PERMISSION_MANAGEMENT ✓
-- ID 33: SUBSCRIPTION_MANAGEMENT ✓
-- ID 34: DEBIT_NOTE_CREATE ✓
-- ID 35: MENU_MANAGEMENT ✓
-- ID 57: Inicio - has URL '/welcome' but might not need feature_code (home page)
-- ID 60: SUBSCRIPTION_MANAGEMENT ✓
-- ID 7: COMPANY_USER_MANAGEMENT ✓

-- Menu ID 57: "Inicio" - This is a home/welcome page, might not need feature_code
-- But if it needs protection, we should create a feature for it
-- For now, let's leave it as is (no feature_code) since it's typically public

-- ============================================
-- STEP 4: Final cleanup - ensure parent menus don't have feature_code
-- ============================================

-- Explicitly set feature_code to NULL for known parent menus
UPDATE public.menu
SET feature_code = NULL
WHERE id_menu IN (
    1,   -- Cliente (parent)
    2,   -- Producto (parent)
    3,   -- Compañía (parent)
    4,   -- Usuario (parent)
    5,   -- Facturación Electrónica (parent)
    16,  -- Administración Global (parent)
    20,  -- Reportes (parent)
    21,  -- Reportes operativos (parent)
    26,  -- Reporte Novedades (parent)
    28   -- Reportes comerciales (parent)
)
AND status = true;

-- ============================================
-- STEP 5: Verify integrity
-- ============================================

DO $$
DECLARE
    parent_with_feature_count INTEGER;
    leaf_without_feature_count INTEGER;
BEGIN
    -- Check if any parent menu has feature_code
    SELECT COUNT(*)
    INTO parent_with_feature_count
    FROM public.menu m1
    WHERE m1.feature_code IS NOT NULL
    AND EXISTS (
        SELECT 1 FROM public.menu m2 
        WHERE m2.parent_id_menu = m1.id_menu
    );
    
    IF parent_with_feature_count > 0 THEN
        RAISE WARNING 'Found % parent menus with feature_code. This should be reviewed.', parent_with_feature_count;
    END IF;
    
    -- Check if any leaf menu (with URL) is missing feature_code
    SELECT COUNT(*)
    INTO leaf_without_feature_count
    FROM public.menu m
    WHERE m.url IS NOT NULL
    AND m.url != ''
    AND m.feature_code IS NULL
    AND m.status = true
    AND NOT EXISTS (
        SELECT 1 FROM public.menu child 
        WHERE child.parent_id_menu = m.id_menu
    );
    
    IF leaf_without_feature_count > 0 THEN
        RAISE WARNING 'Found % leaf menus without feature_code. These should be reviewed.', leaf_without_feature_count;
    END IF;
    
    RAISE NOTICE '✅ Menu feature_code cleanup completed';
END $$;

