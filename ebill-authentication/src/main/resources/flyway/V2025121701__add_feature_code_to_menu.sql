-- ============================================
-- Script: Add feature_code column to menu table
-- Date: 2025-12-17
-- Purpose: Enable feature-based permission validation for menu items
-- Links menu items to feature_permission table for plan-based access control
-- ============================================

-- Step 1: Add feature_code column to menu table
ALTER TABLE public.menu
ADD COLUMN feature_code VARCHAR(50);

-- Step 2: Add foreign key constraint to feature_permission
-- This ensures data integrity - menu items can only reference valid features
ALTER TABLE public.menu
ADD CONSTRAINT fk_menu_feature_code
    FOREIGN KEY (feature_code)
    REFERENCES public.feature_permission(feature_code)
    ON UPDATE CASCADE
    ON DELETE SET NULL;

-- Step 3: Create index for better query performance
CREATE INDEX idx_menu_feature_code ON public.menu(feature_code);

-- Step 4: Add comment for documentation
COMMENT ON COLUMN public.menu.feature_code IS 'Feature permission code required to access this menu item. References feature_permission table.';

-- ============================================
-- Step 5: Populate feature_code for existing menus
-- ============================================

-- Module: INVOICING (Facturación)
-- Menu ID 6: Facturas
UPDATE public.menu 
SET feature_code = 'INVOICE_CREATE' 
WHERE id_menu = 6;

-- Module: CREDIT_NOTES (Notas de Crédito)
-- Menu ID 19: Nota de crédito
UPDATE public.menu 
SET feature_code = 'CREDIT_NOTE_CREATE' 
WHERE id_menu = 19;

-- Module: CLIENTS (Clientes)
-- Menu ID 8: Gestionar clientes
UPDATE public.menu 
SET feature_code = 'CLIENT_CREATE' 
WHERE id_menu = 8;

-- Module: ADMINISTRATION (Administración)
-- Menu ID 7: Gestionar usuarios por compañía
UPDATE public.menu 
SET feature_code = 'USER_MANAGEMENT' 
WHERE id_menu = 7;

-- Menu ID 11: Gestionar usuario
UPDATE public.menu 
SET feature_code = 'USER_MANAGEMENT' 
WHERE id_menu = 11;

-- Menu ID 10: Gestionar compañía
UPDATE public.menu 
SET feature_code = 'COMPANY_SETTINGS' 
WHERE id_menu = 10;

-- Menu ID 12: Gestionar establecimiento (Establishments)
UPDATE public.menu 
SET feature_code = 'ESTABLISHMENT_MANAGEMENT' 
WHERE id_menu = 12 AND name ILIKE '%establecimiento%';

-- Menu ID 13: Gestionar sucursal (Branches)
UPDATE public.menu 
SET feature_code = 'ESTABLISHMENT_MANAGEMENT' 
WHERE id_menu = 13 AND name ILIKE '%sucursal%';

-- Menu ID 14: Gestionar sucursal (otra referencia)
UPDATE public.menu 
SET feature_code = 'ESTABLISHMENT_MANAGEMENT' 
WHERE id_menu = 14 AND name ILIKE '%sucursal%';

-- Menu ID 15: Gestionar punto de emisión (Invoice Points)
UPDATE public.menu 
SET feature_code = 'ESTABLISHMENT_MANAGEMENT' 
WHERE id_menu = 15 AND name ILIKE '%punto%emisi%';

-- Module: REPORTS (Reportes)
-- Menu ID 22: Ventas por periodo
UPDATE public.menu 
SET feature_code = 'REPORT_OPERATIONAL' 
WHERE id_menu = 22;

-- Menu ID 23: Ventas por cliente
UPDATE public.menu 
SET feature_code = 'REPORT_OPERATIONAL' 
WHERE id_menu = 23;

-- Menu ID 24: Ventas por producto
UPDATE public.menu 
SET feature_code = 'REPORT_OPERATIONAL' 
WHERE id_menu = 24;

-- Menu ID 25: Reporte de notas de crédito
UPDATE public.menu 
SET feature_code = 'REPORT_OPERATIONAL' 
WHERE id_menu = 25;

-- Menu ID 27: Reporte de facturas pendientes de envío
UPDATE public.menu 
SET feature_code = 'REPORT_OPERATIONAL' 
WHERE id_menu = 27;

-- Menu ID 29: Dashboard de ventas (Advanced)
UPDATE public.menu 
SET feature_code = 'REPORT_ADVANCED' 
WHERE id_menu = 29;

-- Menu ID 30: Comparativo por sucursal / punto de emisión (Advanced)
UPDATE public.menu 
SET feature_code = 'REPORT_ADVANCED' 
WHERE id_menu = 30;

-- Menu ID 31: Reporte de formas de pago
UPDATE public.menu 
SET feature_code = 'REPORT_SALES' 
WHERE id_menu = 31;

-- Module: PRODUCTS (Productos)
-- Menu ID 9: Gestionar productos
-- Note: We need to add PRODUCT_MANAGEMENT feature first if it doesn't exist
-- For now, we'll leave it NULL or map it to an existing feature
-- UPDATE public.menu SET feature_code = 'PRODUCT_MANAGEMENT' WHERE id_menu = 9;

-- Note: Parent menus (menus with subMenus) typically don't need feature_code
-- They are shown/hidden based on whether the user has access to ANY of their children
-- Examples: Menu IDs 1, 2, 3, 4, 5, 20, 21, 26, 28 (parent containers)
-- These will remain NULL

-- ============================================
-- Step 6: Add PRODUCT_MANAGEMENT feature if needed
-- ============================================

-- Insert PRODUCT_MANAGEMENT feature if it doesn't exist
INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
SELECT 'PRODUCT_MANAGEMENT', 'Gestión de Productos', 'Crear y gestionar productos', 'PRODUCTS', ARRAY[1,2,3], ARRAY['FREE','BASIC','PROFESSIONAL','ENTERPRISE'], true
WHERE NOT EXISTS (
    SELECT 1 FROM public.feature_permission WHERE feature_code = 'PRODUCT_MANAGEMENT'
);

-- Now update the products menu
UPDATE public.menu 
SET feature_code = 'PRODUCT_MANAGEMENT' 
WHERE id_menu = 9;

-- ============================================
-- VERIFICATION QUERIES (commented out - run manually for testing)
-- ============================================

-- Count menus with feature_code assigned
-- SELECT COUNT(*) as menus_with_feature_code FROM public.menu WHERE feature_code IS NOT NULL;

-- Count menus without feature_code (should be parent menus only)
-- SELECT COUNT(*) as menus_without_feature_code FROM public.menu WHERE feature_code IS NULL AND status = true;

-- List all menus with their feature codes
-- SELECT m.id_menu, m.name, m.url, m.feature_code, m.parent_id_menu
-- FROM public.menu m
-- WHERE m.status = true
-- ORDER BY m.parent_id_menu NULLS FIRST, m.id_menu;

-- Verify all feature codes are valid (no orphaned references)
-- SELECT m.id_menu, m.name, m.feature_code
-- FROM public.menu m
-- LEFT JOIN public.feature_permission fp ON m.feature_code = fp.feature_code
-- WHERE m.feature_code IS NOT NULL
--   AND fp.feature_code IS NULL;
-- -- Should return 0 rows

-- Show menu hierarchy with feature codes
-- WITH RECURSIVE menu_tree AS (
--     SELECT id_menu, name, url, feature_code, parent_id_menu, 0 as level, 
--            CAST(id_menu AS TEXT) as path
--     FROM menu 
--     WHERE parent_id_menu IS NULL AND status = true
--     UNION ALL
--     SELECT m.id_menu, m.name, m.url, m.feature_code, m.parent_id_menu, mt.level + 1,
--            mt.path || '->' || CAST(m.id_menu AS TEXT)
--     FROM menu m
--     INNER JOIN menu_tree mt ON m.parent_id_menu = mt.id_menu
--     WHERE m.status = true
-- )
-- SELECT REPEAT('  ', level) || name as menu_name, url, feature_code, level
-- FROM menu_tree
-- ORDER BY path;
