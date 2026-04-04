-- ============================================
-- Script: Add Delivery Guide menu (Guías de Remisión)
-- Date: 2025-12-29
-- Purpose: Create menu item for delivery guides management
-- Only super_admin (role 1) has access
-- Uses helper functions for safe, idempotent execution
-- Note: Menu helper functions are defined in V2025122700__create_admin_feature_menus.sql
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
    'DEBIT_NOTE_CREATE',
    'Crear Notas de debito',
    'Crear Notas de debito',
    'INVOICING', 
    ARRAY[1,2,3], -- Only super_admin (role 1)
    ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'], -- Available in all plans
    TRUE
)
ON CONFLICT (feature_code) DO NOTHING;
-- ============================================
-- STEP 2: Create menu using helper function
-- Parent: Facturación Electrónica (ID: 5)
-- ============================================
update menu set feature_code='DEBIT_NOTE_CREATE' where name='Nota de débito' and url='/debit-note';

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
    'CARRIER_CREATE',
    'Crear Transportistas',
    'Crear Transportistas',
    'CLIENTS', -- Same module as invoices and credit notes
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
        1,                              -- parent_id_menu: Facturación Electrónica
        'Gestionar Transportistas',           -- name
        '/carriers',             -- url
        'badge',               -- icon (shipping/transport icon)
        'CARRIER_CREATE'         -- feature_code
    );
    
    RAISE NOTICE '✅ Menu "Transportistas" creado/actualizado - ID: %', menu_id;
    
    -- Assign menu to super_admin (role 1) using helper function
    assignment_id := assign_menu_to_role_safe(menu_id, 1);
    
    RAISE NOTICE '✅ Menu "Guías de Remisión" asignado a super_admin - Assignment ID: %', assignment_id;
END $$;

