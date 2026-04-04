-- ============================================
-- Script: Add ROLE_PERMISSION_MANAGEMENT feature
-- Date: 2025-12-18
-- Purpose: Create feature for managing role permissions
-- Only super_admin (role 1) can manage role permissions
-- ============================================

-- Insert the new feature
INSERT INTO public.feature_permission 
    (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES 
    (
        'ROLE_PERMISSION_MANAGEMENT',
        'Gestión de Permisos de Roles',
        'Permite gestionar y asignar permisos (features) a diferentes roles del sistema',
        'ADMINISTRATION',
        ARRAY[1], -- Solo super_admin
        ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],
        true
    )
ON CONFLICT (feature_code) DO NOTHING;

-- ============================================
-- VERIFICATION QUERIES (commented - run manually)
-- ============================================

-- Verify the feature was created
-- SELECT * FROM feature_permission WHERE feature_code = 'ROLE_PERMISSION_MANAGEMENT';

-- Expected output:
-- feature_code: ROLE_PERMISSION_MANAGEMENT
-- feature_name: Gestión de Permisos de Roles
-- allowed_roles: {1}
-- required_plans: {FREE,BASIC,PROFESSIONAL,ENTERPRISE}
-- is_active: true
