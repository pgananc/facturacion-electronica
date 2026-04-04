-- ============================================
-- Script: Add Proforma
-- Date: 2026-03-09
-- Purpose: Create menu item for delivery guides management
-- Only super_admin (role 1) has access
-- Uses helper functions for safe, idempotent execution
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
    'PROFORMA_CREATE',
    'Crear Proforma',
    'Crear Proforma',
    'INVOICING', 
    ARRAY[1,2,3], -- Only super_admin (role 1)
    ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'], -- Available in all plans
    TRUE
)
ON CONFLICT (feature_code) DO NOTHING;



DO $$
DECLARE
    menu_id INTEGER;
    assignment_id BIGINT;
BEGIN
    -- Create menu using helper function
    menu_id := insert_menu_safe(
        5,                              -- parent_id_menu: Facturación Electrónica
        'Proforma',           -- name
        '/proforma',             -- url
        'request_quote',               -- icon (shipping/transport icon)
        'PROFORMA_CREATE'         -- feature_code
    );
    
    RAISE NOTICE '✅ Menu "Proforma - ID: %', menu_id;
    
    -- Assign menu to super_admin (role 1) using helper function
    assignment_id := assign_menu_to_role_safe(menu_id, 1);
    
    RAISE NOTICE '✅ Menu "Proforma ID: %', assignment_id;
END $$;

