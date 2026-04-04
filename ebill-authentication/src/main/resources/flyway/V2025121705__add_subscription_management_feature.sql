-- ============================================
-- Script: Add SUBSCRIPTION_MANAGEMENT feature
-- Date: 2025-12-18
-- Purpose: Create feature for managing company subscriptions
-- Only super_admin (role 1) can manage subscriptions
-- ============================================

-- Insert the new feature
INSERT INTO public.feature_permission 
    (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES 
    (
        'SUBSCRIPTION_MANAGEMENT',
        'Gestión de Suscripciones',
        'Permite gestionar suscripciones de empresas: listar, ver detalles, cambiar plan, extender vigencia y cancelar',
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
-- SELECT * FROM feature_permission WHERE feature_code = 'SUBSCRIPTION_MANAGEMENT';

-- Expected output:
-- feature_code: SUBSCRIPTION_MANAGEMENT
-- feature_name: Gestión de Suscripciones
-- allowed_roles: {1}
-- required_plans: {FREE,BASIC,PROFESSIONAL,ENTERPRISE}
-- is_active: true
