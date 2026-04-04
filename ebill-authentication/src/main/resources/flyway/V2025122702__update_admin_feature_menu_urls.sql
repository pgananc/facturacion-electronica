-- ============================================
-- Script: Update admin feature menu URLs
-- Date: 2025-12-18
-- Purpose: Correct URLs for ROLE_PERMISSION_MANAGEMENT and SUBSCRIPTION_MANAGEMENT menus
-- The URLs should follow the /admin/* pattern for administrative features
-- ============================================

-- ============================================
-- UPDATE: Gestión de Permisos URL
-- ============================================

UPDATE public.menu 
SET url = '/admin/role-permissions'
WHERE feature_code = 'ROLE_PERMISSION_MANAGEMENT';

-- ============================================
-- UPDATE: Gestión de Suscripciones URL
-- ============================================

UPDATE public.menu 
SET url = '/admin/subscriptions'
WHERE feature_code = 'SUBSCRIPTION_MANAGEMENT';

-- ============================================
-- VERIFICATION QUERIES (commented - run manually)
-- ============================================

-- Verify the URLs were updated correctly
-- SELECT id_menu, name, url, feature_code, parent_id_menu
-- FROM public.menu 
-- WHERE feature_code IN ('ROLE_PERMISSION_MANAGEMENT', 'SUBSCRIPTION_MANAGEMENT');

-- Expected output:
-- id_menu: 32, name: Gestión de Permisos, url: /admin/role-permissions, feature_code: ROLE_PERMISSION_MANAGEMENT
-- id_menu: 33, name: Gestión de Suscripciones, url: /admin/subscriptions, feature_code: SUBSCRIPTION_MANAGEMENT

-- Verify no other menus were affected
-- SELECT id_menu, name, url, feature_code
-- FROM public.menu 
-- WHERE url LIKE '%permissions%' OR url LIKE '%subscriptions%';

-- Expected: Should only show the 2 menus we intended to update (IDs 32 and 33)
