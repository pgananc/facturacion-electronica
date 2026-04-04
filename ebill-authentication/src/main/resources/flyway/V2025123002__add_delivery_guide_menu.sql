-- ============================================
-- Script: Add Delivery Guide menu (Guías de Remisión)
-- Date: 2025-12-29
-- Purpose: Create menu item for delivery guides management
-- Only super_admin (role 1) has access
-- Uses helper functions for safe, idempotent execution
-- Note: Menu helper functions are defined in V2025122700__create_admin_feature_menus.sql
-- ============================================

-- ============================================
-- STEP 1: Create DELIVERY_GUIDE_CREATE feature if not exists
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
    'DELIVERY_GUIDE_CREATE',
    'Crear Guías de Remisión',
    'Crear y emitir guías de remisión',
    'INVOICING', -- Same module as invoices and credit notes
    ARRAY[1], -- Only super_admin (role 1)
    ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'], -- Available in all plans
    TRUE
)
ON CONFLICT (feature_code) DO NOTHING;

-- ============================================
-- STEP 2: Create menu using helper function
-- Parent: Facturación Electrónica (ID: 5)
-- ============================================

DO $$
DECLARE
    menu_id INTEGER;
    assignment_id BIGINT;
BEGIN
    -- Create menu using helper function
    menu_id := insert_menu_safe(
        5,                              -- parent_id_menu: Facturación Electrónica
        'Guías de Remisión',           -- name
        '/delivery-guides',             -- url
        'local_shipping',               -- icon (shipping/transport icon)
        'DELIVERY_GUIDE_CREATE'         -- feature_code
    );
    
    RAISE NOTICE '✅ Menu "Guías de Remisión" creado/actualizado - ID: %', menu_id;
    
    -- Assign menu to super_admin (role 1) using helper function
    assignment_id := assign_menu_to_role_safe(menu_id, 1);
    
    RAISE NOTICE '✅ Menu "Guías de Remisión" asignado a super_admin - Assignment ID: %', assignment_id;
END $$;

