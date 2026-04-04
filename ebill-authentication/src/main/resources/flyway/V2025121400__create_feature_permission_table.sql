-- ============================================
-- Script: Create feature_permission table
-- Date: 2025-12-14
-- Purpose: Feature and permission management system
-- ============================================

-- Create sequence for feature_permission
CREATE SEQUENCE IF NOT EXISTS public.feature_permission_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Create feature_permission table
CREATE TABLE IF NOT EXISTS public.feature_permission
(
    id_permission    BIGINT       NOT NULL DEFAULT nextval('feature_permission_seq'::regclass),
    feature_code     VARCHAR(50)  NOT NULL UNIQUE,
    feature_name     VARCHAR(100) NOT NULL,
    description      TEXT,
    module           VARCHAR(50),
    allowed_roles    INTEGER[],
    required_plans   VARCHAR(50)[],
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_feature_permission PRIMARY KEY (id_permission)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_feature_permission_code ON public.feature_permission (feature_code);
CREATE INDEX IF NOT EXISTS idx_feature_permission_active ON public.feature_permission (is_active);
CREATE INDEX IF NOT EXISTS idx_feature_permission_module ON public.feature_permission (module);
CREATE INDEX IF NOT EXISTS idx_feature_permission_allowed_roles ON public.feature_permission USING GIN (allowed_roles);

-- Add comments for better documentation
COMMENT ON TABLE public.feature_permission IS 'Feature permissions and plan requirements table';
COMMENT ON COLUMN public.feature_permission.feature_code IS 'Unique feature identifier code';
COMMENT ON COLUMN public.feature_permission.feature_name IS 'Human-readable feature name';
COMMENT ON COLUMN public.feature_permission.allowed_roles IS 'Array of role IDs that can access this feature';
COMMENT ON COLUMN public.feature_permission.required_plans IS 'Array of plan codes required for this feature';

-- ============================================
-- INSERT 15 BASE FEATURES
-- ============================================

-- Module: Invoicing
INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('INVOICE_CREATE', 'Crear Facturas', 'Crear y emitir facturas electrónicas', 'INVOICING', ARRAY [1, 2, 3], ARRAY ['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'], TRUE);

INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('INVOICE_VOID', 'Anular Facturas', 'Anular facturas emitidas', 'INVOICING', ARRAY [1, 2], ARRAY ['BASIC', 'PROFESSIONAL', 'ENTERPRISE'], TRUE);

INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('INVOICE_EXPORT', 'Exportar Facturas', 'Exportar facturas en diferentes formatos', 'INVOICING', ARRAY [1, 2, 3], ARRAY ['PROFESSIONAL', 'ENTERPRISE'], TRUE);

-- Module: Credit Notes
INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('CREDIT_NOTE_CREATE', 'Crear Notas de Crédito', 'Crear y emitir notas de crédito', 'CREDIT_NOTES', ARRAY [1, 2, 3], ARRAY ['BASIC', 'PROFESSIONAL', 'ENTERPRISE'], TRUE);

INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('CREDIT_NOTE_VOID', 'Anular Notas de Crédito', 'Anular notas de crédito emitidas', 'CREDIT_NOTES', ARRAY [1, 2], ARRAY ['BASIC', 'PROFESSIONAL', 'ENTERPRISE'], TRUE);

-- Module: Clients
INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('CLIENT_CREATE', 'Crear Clientes', 'Crear y gestionar información de clientes', 'CLIENTS', ARRAY [1, 2, 3], ARRAY ['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'], TRUE);

INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('CLIENT_IMPORT', 'Importar Clientes', 'Importar clientes desde archivos externos', 'CLIENTS', ARRAY [1, 2], ARRAY ['PROFESSIONAL', 'ENTERPRISE'], TRUE);

-- Module: Reports
INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('REPORT_OPERATIONAL', 'Reportes Operativos', 'Acceder a reportes operativos', 'REPORTS', ARRAY [1, 2, 3], ARRAY ['BASIC', 'PROFESSIONAL', 'ENTERPRISE'], TRUE);

INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('REPORT_SALES', 'Reportes Comerciales', 'Acceder a reportes comerciales y análisis de ventas', 'REPORTS', ARRAY [1, 2], ARRAY ['PROFESSIONAL', 'ENTERPRISE'], TRUE);

INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('REPORT_ADVANCED', 'Reportes Avanzados', 'Acceder a reportes avanzados y personalizados', 'REPORTS', ARRAY [1, 2], ARRAY ['ENTERPRISE'], TRUE);

-- Module: Administration
INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('USER_MANAGEMENT', 'Gestión de Usuarios', 'Crear y administrar usuarios del sistema', 'ADMINISTRATION', ARRAY [1, 2], ARRAY ['BASIC', 'PROFESSIONAL', 'ENTERPRISE'], TRUE);

INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('COMPANY_SETTINGS', 'Configuración de Empresa', 'Gestionar configuración de la empresa', 'ADMINISTRATION', ARRAY [1, 2], ARRAY ['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'], TRUE);

INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('ESTABLISHMENT_MANAGEMENT', 'Gestión de Establecimientos', 'Gestionar establecimientos y puntos de emisión', 'ADMINISTRATION', ARRAY [1, 2], ARRAY ['BASIC', 'PROFESSIONAL', 'ENTERPRISE'], TRUE);

-- Module: API Integration
INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('API_ACCESS', 'Acceso API', 'Acceso a la API REST para integración', 'API', ARRAY [1, 2], ARRAY ['PROFESSIONAL', 'ENTERPRISE'], TRUE);

INSERT INTO public.feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('WEBHOOK_CONFIG', 'Configuración de Webhooks', 'Configurar webhooks para notificaciones', 'API', ARRAY [1], ARRAY ['ENTERPRISE'], TRUE);

-- ============================================
-- VERIFICATION QUERIES (for testing)
-- ============================================

-- Count total features
-- SELECT COUNT(*) as total_features FROM public.feature_permission;

-- List all features by module
-- SELECT module, COUNT(*) as feature_count FROM public.feature_permission GROUP BY module ORDER BY module;

-- Show features by role (example for role 3)
-- SELECT feature_code, feature_name, module FROM public.feature_permission WHERE 3 = ANY(allowed_roles);

-- Show features by plan (example for BASIC plan)
-- SELECT feature_code, feature_name, module FROM public.feature_permission WHERE 'BASIC' = ANY(required_plans);
