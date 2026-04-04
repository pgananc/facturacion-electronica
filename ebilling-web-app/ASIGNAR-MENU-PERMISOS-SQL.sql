-- ===================================================================
-- ASIGNAR MENÚ "Gestión de Permisos" (ID: 32) AL ROL super_admin
-- ===================================================================

-- Paso 1: Verificar que el menú existe
SELECT 
  id_menu,
  parent_id_menu,
  name,
  url,
  icon,
  status,
  feature_code
FROM public.menu
WHERE id_menu = 32;

-- Paso 2: Verificar que el rol super_admin existe
SELECT 
  id_role,
  name,
  description
FROM public.role
WHERE name = 'super_admin';

-- Paso 3: Verificar si ya está asignado
SELECT 
  rm.id_role_menu,
  r.name AS role_name,
  m.name AS menu_name,
  m.url AS menu_url
FROM public.role_menu rm
INNER JOIN public.role r ON rm.id_role = r.id_role
INNER JOIN public.menu m ON rm.id_menu = m.id_menu
WHERE r.name = 'super_admin'
  AND m.id_menu = 32;

-- Paso 4: ASIGNAR el menú al rol (solo si no existe)
INSERT INTO public.role_menu (
  id_role,
  id_menu
)
SELECT 
  r.id_role,
  32  -- ID del menú "Gestión de Permisos"
FROM public.role r
WHERE r.name = 'super_admin'
  AND NOT EXISTS (
    SELECT 1 
    FROM public.role_menu rm 
    WHERE rm.id_role = r.id_role 
      AND rm.id_menu = 32
  );

-- Paso 5: Verificar que se asignó correctamente
SELECT 
  rm.id_role_menu,
  r.name AS role_name,
  r.id_role,
  m.id_menu,
  m.name AS menu_name,
  m.url AS menu_url,
  m.feature_code
FROM public.role_menu rm
INNER JOIN public.role r ON rm.id_role = r.id_role
INNER JOIN public.menu m ON rm.id_menu = m.id_menu
WHERE r.name = 'super_admin'
  AND m.id_menu = 32;

-- ===================================================================
-- VERIFICACIÓN COMPLETA: Ver todos los menús de Administración Global
-- ===================================================================
SELECT 
  m.id_menu,
  m.parent_id_menu,
  m.name,
  m.url,
  m.icon,
  m.status,
  m.feature_code,
  CASE 
    WHEN EXISTS (
      SELECT 1 
      FROM public.role_menu rm 
      WHERE rm.id_menu = m.id_menu 
        AND rm.id_role = (SELECT id_role FROM public.role WHERE name = 'super_admin')
    ) THEN '✅ Asignado'
    ELSE '❌ NO asignado'
  END AS asignacion_super_admin
FROM public.menu m
WHERE m.parent_id_menu = 16 OR m.id_menu = 16
ORDER BY m.parent_id_menu NULLS FIRST, m.id_menu;

