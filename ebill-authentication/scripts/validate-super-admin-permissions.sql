-- ============================================
-- Script: Validate Super Admin Permissions
-- Date: 2025-12-18
-- Purpose: Comprehensive validation of all super_admin (role 1) permissions
-- ============================================

-- ============================================
-- 1. COUNT: Total menus for super_admin
-- ============================================
SELECT 
    '1. Total Menús Super Admin' as validation,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) >= 33 THEN '✅ CORRECTO'
        ELSE '❌ ERROR - Debería tener 33 menús'
    END as status
FROM menu_role
WHERE id_role = 1 AND status = true;

-- ============================================
-- 2. VERIFY: New administrative menus (32, 33)
-- ============================================
SELECT 
    '2. Menús Administrativos Nuevos' as validation,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) = 2 THEN '✅ CORRECTO'
        ELSE '❌ ERROR - Faltan menús 32 o 33'
    END as status
FROM menu_role mr
INNER JOIN menu m ON m.id_menu = mr.id_menu
WHERE mr.id_role = 1 
  AND mr.id_menu IN (32, 33)
  AND mr.status = true;

-- ============================================
-- 3. DETAIL: List all super_admin menus
-- ============================================
SELECT 
    '3. Detalle de Todos los Menús' as validation,
    m.id_menu,
    m.parent_id_menu,
    m.name,
    m.url,
    m.feature_code,
    CASE 
        WHEN m.parent_id_menu IS NULL THEN '📁 GRUPO'
        ELSE '📄 SUBMENU'
    END as tipo
FROM menu_role mr
INNER JOIN menu m ON m.id_menu = mr.id_menu
WHERE mr.id_role = 1 AND mr.status = true AND m.status = true
ORDER BY m.parent_id_menu NULLS FIRST, m.id_menu;

-- ============================================
-- 4. VERIFY: Features with super_admin role
-- ============================================
SELECT 
    '4. Features con Super Admin en allowed_roles' as validation,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) >= 16 THEN '✅ CORRECTO'
        ELSE '❌ ERROR - Debería tener al menos 16 features'
    END as status
FROM feature_permission
WHERE 1 = ANY(allowed_roles);

-- ============================================
-- 5. DETAIL: All features for super_admin
-- ============================================
SELECT 
    '5. Detalle de Todos los Features' as validation,
    feature_code,
    feature_name,
    module,
    allowed_roles,
    CASE 
        WHEN feature_code IN ('ROLE_PERMISSION_MANAGEMENT', 'SUBSCRIPTION_MANAGEMENT') 
        THEN '🆕 NUEVO'
        ELSE '📌 EXISTENTE'
    END as tipo
FROM feature_permission
WHERE 1 = ANY(allowed_roles)
ORDER BY 
    CASE WHEN feature_code IN ('ROLE_PERMISSION_MANAGEMENT', 'SUBSCRIPTION_MANAGEMENT') THEN 0 ELSE 1 END,
    feature_code;

-- ============================================
-- 6. VERIFY: New features exist
-- ============================================
SELECT 
    '6. Nuevos Features Creados' as validation,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) = 2 THEN '✅ CORRECTO'
        ELSE '❌ ERROR - Faltan ROLE_PERMISSION_MANAGEMENT o SUBSCRIPTION_MANAGEMENT'
    END as status
FROM feature_permission
WHERE feature_code IN ('ROLE_PERMISSION_MANAGEMENT', 'SUBSCRIPTION_MANAGEMENT')
  AND 1 = ANY(allowed_roles);

-- ============================================
-- 7. VERIFY: Menu-Feature relationship
-- ============================================
SELECT 
    '7. Relación Menu-Feature para nuevos menús' as validation,
    m.id_menu,
    m.name,
    m.feature_code,
    fp.feature_name,
    CASE 
        WHEN fp.feature_code IS NOT NULL AND 1 = ANY(fp.allowed_roles) THEN '✅ CORRECTO'
        ELSE '❌ ERROR - Feature no existe o super_admin no tiene acceso'
    END as status
FROM menu m
LEFT JOIN feature_permission fp ON fp.feature_code = m.feature_code
WHERE m.id_menu IN (32, 33);

-- ============================================
-- 8. VERIFY: Administración Global group and children
-- ============================================
SELECT 
    '8. Menús bajo Administración Global' as validation,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) >= 4 THEN '✅ CORRECTO'
        ELSE '❌ ERROR - Faltan menús en Administración Global'
    END as status
FROM menu_role mr
INNER JOIN menu m ON m.id_menu = mr.id_menu
WHERE mr.id_role = 1 
  AND (m.id_menu = 16 OR m.parent_id_menu = 16)
  AND mr.status = true;

-- ============================================
-- 9. DETAIL: Administración Global hierarchy
-- ============================================
SELECT 
    '9. Detalle Administración Global' as validation,
    m.id_menu,
    m.name,
    m.url,
    m.feature_code,
    CASE 
        WHEN m.id_menu = 16 THEN '📁 GRUPO PADRE'
        ELSE '📄 SUBMENU'
    END as tipo
FROM menu_role mr
INNER JOIN menu m ON m.id_menu = mr.id_menu
WHERE mr.id_role = 1 
  AND (m.id_menu = 16 OR m.parent_id_menu = 16)
  AND mr.status = true
ORDER BY m.id_menu;

-- ============================================
-- 10. VERIFY: No missing features for super_admin
-- ============================================
SELECT 
    '10. Features sin Super Admin asignado' as validation,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ CORRECTO - Todos los features incluyen super_admin'
        ELSE '⚠️ ADVERTENCIA - Algunos features no incluyen super_admin'
    END as status
FROM feature_permission
WHERE NOT (1 = ANY(allowed_roles));

-- ============================================
-- 11. DETAIL: Features without super_admin (if any)
-- ============================================
SELECT 
    '11. Features que NO incluyen Super Admin' as validation,
    feature_code,
    feature_name,
    allowed_roles,
    '⚠️ Este feature NO incluye rol 1' as nota
FROM feature_permission
WHERE NOT (1 = ANY(allowed_roles));

-- ============================================
-- 12. VERIFY: Menu sequence is synchronized
-- ============================================
SELECT 
    '12. Estado de Secuencia menu_seq' as validation,
    last_value as ultimo_valor_secuencia,
    (SELECT MAX(id_menu) FROM menu) as max_id_en_tabla,
    CASE 
        WHEN last_value >= (SELECT MAX(id_menu) FROM menu) THEN '✅ SINCRONIZADA'
        ELSE '❌ DESINCRONIZADA'
    END as status
FROM menu_seq;

-- ============================================
-- 13. VERIFY: menu_role sequence is synchronized
-- ============================================
SELECT 
    '13. Estado de Secuencia menu_role_seq' as validation,
    last_value as ultimo_valor_secuencia,
    (SELECT MAX(id_menu_role) FROM menu_role) as max_id_en_tabla,
    CASE 
        WHEN last_value >= (SELECT MAX(id_menu_role) FROM menu_role) THEN '✅ SINCRONIZADA'
        ELSE '❌ DESINCRONIZADA'
    END as status
FROM menu_role_seq;

-- ============================================
-- 14. SUMMARY: Super Admin vs Other Roles
-- ============================================
SELECT 
    '14. Comparación de Roles' as validation,
    r.id_role,
    r.name,
    COUNT(DISTINCT mr.id_menu) as total_menus,
    CASE 
        WHEN r.id_role = 1 THEN '👑 SUPER ADMIN - Debería tener ~33 menús'
        WHEN r.id_role = 3 THEN '🔧 ADMIN - Debería tener ~28 menús'
        ELSE '👤 OTRO ROL'
    END as descripcion
FROM role r
LEFT JOIN menu_role mr ON mr.id_role = r.id_role AND mr.status = true
GROUP BY r.id_role, r.name
ORDER BY r.id_role;

-- ============================================
-- 15. VERIFY: Orphaned menus (menus without role assignment)
-- ============================================
SELECT 
    '15. Menús sin asignación a ningún rol' as validation,
    COUNT(*) as count,
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ CORRECTO - Todos los menús están asignados'
        ELSE '⚠️ ADVERTENCIA - Hay menús sin asignar'
    END as status
FROM menu m
LEFT JOIN menu_role mr ON mr.id_menu = m.id_menu
WHERE m.status = true AND mr.id_menu_role IS NULL;

-- ============================================
-- 16. DETAIL: Check specific new features are properly configured
-- ============================================
SELECT 
    '16. Configuración de Nuevos Features' as validation,
    fp.feature_code,
    fp.feature_name,
    fp.allowed_roles,
    fp.required_plans,
    m.id_menu as menu_id,
    m.name as menu_name,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM menu_role mr 
            WHERE mr.id_menu = m.id_menu AND mr.id_role = 1
        ) THEN '✅ Menu asignado a super_admin'
        ELSE '❌ Menu NO asignado a super_admin'
    END as menu_status
FROM feature_permission fp
LEFT JOIN menu m ON m.feature_code = fp.feature_code
WHERE fp.feature_code IN ('ROLE_PERMISSION_MANAGEMENT', 'SUBSCRIPTION_MANAGEMENT');

-- ============================================
-- END OF VALIDATIONS
-- ============================================


