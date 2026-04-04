-- ============================================
-- Script: Add Admin role (id=3) to feature permissions
-- Date: 2025-12-17
-- Purpose: Grant feature access to Admin role for company-level operations
-- Admin gets 14 features (excludes API_ACCESS and WEBHOOK_CONFIG)
-- ============================================

-- ============================================
-- ADMIN ROLE (id=3) - FEATURE PERMISSIONS
-- Total: 14 features
-- ============================================

-- ============================================
-- MODULE: INVOICING (3 features)
-- ============================================

-- INVOICE_CREATE: Crear y emitir facturas electrónicas
-- Current: ARRAY[1, 2, 3] (already includes Admin)
-- Action: Verify it exists, update if needed
UPDATE public.feature_permission
SET allowed_roles = ARRAY[1, 2, 3]
WHERE feature_code = 'INVOICE_CREATE'
  AND NOT (3 = ANY(allowed_roles));

-- INVOICE_VOID: Anular facturas emitidas
-- Current: ARRAY[1, 2] 
-- Action: Add role 3
UPDATE public.feature_permission
SET allowed_roles = array_append(allowed_roles, 3)
WHERE feature_code = 'INVOICE_VOID'
  AND NOT (3 = ANY(allowed_roles));

-- INVOICE_EXPORT: Exportar facturas en diferentes formatos
-- Current: ARRAY[1, 2, 3] (already includes Admin)
-- Action: Verify it exists, update if needed
UPDATE public.feature_permission
SET allowed_roles = ARRAY[1, 2, 3]
WHERE feature_code = 'INVOICE_EXPORT'
  AND NOT (3 = ANY(allowed_roles));

-- ============================================
-- MODULE: CREDIT_NOTES (2 features)
-- ============================================

-- CREDIT_NOTE_CREATE: Crear y emitir notas de crédito
-- Current: ARRAY[1, 2, 3] (already includes Admin)
-- Action: Verify it exists, update if needed
UPDATE public.feature_permission
SET allowed_roles = ARRAY[1, 2, 3]
WHERE feature_code = 'CREDIT_NOTE_CREATE'
  AND NOT (3 = ANY(allowed_roles));

-- CREDIT_NOTE_VOID: Anular notas de crédito emitidas
-- Current: ARRAY[1, 2]
-- Action: Add role 3
UPDATE public.feature_permission
SET allowed_roles = array_append(allowed_roles, 3)
WHERE feature_code = 'CREDIT_NOTE_VOID'
  AND NOT (3 = ANY(allowed_roles));

-- ============================================
-- MODULE: CLIENTS (2 features)
-- ============================================

-- CLIENT_CREATE: Crear y gestionar información de clientes
-- Current: ARRAY[1, 2, 3] (already includes Admin)
-- Action: Verify it exists, update if needed
UPDATE public.feature_permission
SET allowed_roles = ARRAY[1, 2, 3]
WHERE feature_code = 'CLIENT_CREATE'
  AND NOT (3 = ANY(allowed_roles));

-- CLIENT_IMPORT: Importar clientes desde archivos externos
-- Current: ARRAY[1, 2]
-- Action: Add role 3
UPDATE public.feature_permission
SET allowed_roles = array_append(allowed_roles, 3)
WHERE feature_code = 'CLIENT_IMPORT'
  AND NOT (3 = ANY(allowed_roles));

-- ============================================
-- MODULE: PRODUCTS (1 feature)
-- ============================================

-- PRODUCT_MANAGEMENT: Crear y gestionar productos
-- Current: ARRAY[1, 2, 3] (already includes Admin)
-- Action: Verify it exists, update if needed
UPDATE public.feature_permission
SET allowed_roles = ARRAY[1, 2, 3]
WHERE feature_code = 'PRODUCT_MANAGEMENT'
  AND NOT (3 = ANY(allowed_roles));

-- ============================================
-- MODULE: REPORTS (3 features)
-- ============================================

-- REPORT_OPERATIONAL: Acceder a reportes operativos
-- Current: ARRAY[1, 2, 3] (already includes Admin)
-- Action: Verify it exists, update if needed
UPDATE public.feature_permission
SET allowed_roles = ARRAY[1, 2, 3]
WHERE feature_code = 'REPORT_OPERATIONAL'
  AND NOT (3 = ANY(allowed_roles));

-- REPORT_SALES: Acceder a reportes comerciales y análisis de ventas
-- Current: ARRAY[1, 2]
-- Action: Add role 3
UPDATE public.feature_permission
SET allowed_roles = array_append(allowed_roles, 3)
WHERE feature_code = 'REPORT_SALES'
  AND NOT (3 = ANY(allowed_roles));

-- REPORT_ADVANCED: Acceder a reportes avanzados y personalizados
-- Current: ARRAY[1, 2]
-- Action: Add role 3
UPDATE public.feature_permission
SET allowed_roles = array_append(allowed_roles, 3)
WHERE feature_code = 'REPORT_ADVANCED'
  AND NOT (3 = ANY(allowed_roles));

-- ============================================
-- MODULE: ADMINISTRATION (3 features)
-- ============================================

-- USER_MANAGEMENT: Crear y administrar usuarios del sistema
-- Current: ARRAY[1, 2]
-- Action: Add role 3
UPDATE public.feature_permission
SET allowed_roles = array_append(allowed_roles, 3)
WHERE feature_code = 'USER_MANAGEMENT'
  AND NOT (3 = ANY(allowed_roles));

-- COMPANY_SETTINGS: Gestionar configuración de la empresa
-- Current: ARRAY[1, 2]
-- Action: Add role 3
UPDATE public.feature_permission
SET allowed_roles = array_append(allowed_roles, 3)
WHERE feature_code = 'COMPANY_SETTINGS'
  AND NOT (3 = ANY(allowed_roles));

-- ESTABLISHMENT_MANAGEMENT: Gestionar establecimientos y puntos de emisión
-- Current: ARRAY[1, 2]
-- Action: Add role 3
UPDATE public.feature_permission
SET allowed_roles = array_append(allowed_roles, 3)
WHERE feature_code = 'ESTABLISHMENT_MANAGEMENT'
  AND NOT (3 = ANY(allowed_roles));

-- ============================================
-- MODULE: API (0 features for Admin)
-- ============================================

-- ❌ API_ACCESS: Acceso a la API REST para integración
-- Current: ARRAY[1, 2]
-- Action: DO NOT ADD role 3 - This is only for super_admin and accountants with API needs

-- ❌ WEBHOOK_CONFIG: Configurar webhooks para notificaciones
-- Current: ARRAY[1]
-- Action: DO NOT ADD role 3 - This is only for super_admin

-- ============================================
-- VERIFICATION QUERIES (commented - run manually for testing)
-- ============================================

-- Count features accessible by Admin role
-- SELECT COUNT(*) as total_features_admin
-- FROM public.feature_permission
-- WHERE 3 = ANY(allowed_roles) AND is_active = true;
-- Expected: 14

-- List all features accessible by Admin role
-- SELECT feature_code, feature_name, module, allowed_roles, required_plans
-- FROM public.feature_permission
-- WHERE 3 = ANY(allowed_roles) AND is_active = true
-- ORDER BY module, feature_code;

-- Verify Admin does NOT have access to restricted features
-- SELECT feature_code, feature_name, allowed_roles
-- FROM public.feature_permission
-- WHERE feature_code IN ('API_ACCESS', 'WEBHOOK_CONFIG')
--   AND 3 = ANY(allowed_roles);
-- Expected: 0 rows

-- Compare features by role
-- SELECT 
--     fp.feature_code,
--     fp.feature_name,
--     CASE WHEN 1 = ANY(fp.allowed_roles) THEN '✓' ELSE '✗' END as super_admin,
--     CASE WHEN 2 = ANY(fp.allowed_roles) THEN '✓' ELSE '✗' END as member,
--     CASE WHEN 3 = ANY(fp.allowed_roles) THEN '✓' ELSE '✗' END as admin,
--     fp.module
-- FROM public.feature_permission fp
-- WHERE fp.is_active = true
-- ORDER BY fp.module, fp.feature_code;
