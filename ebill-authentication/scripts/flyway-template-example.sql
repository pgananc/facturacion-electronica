-- ============================================
-- TEMPLATE: Ejemplo de Migración Flyway usando Funciones Helper
-- ============================================
-- 
-- INSTRUCCIONES:
-- 1. Copia este template para nuevas migraciones
-- 2. Reemplaza los valores según necesites
-- 3. Usa las funciones helper para evitar problemas de IDs
-- 
-- ============================================

-- ============================================
-- Ejemplo 1: Crear un nuevo menú y asignarlo a un rol
-- ============================================

DO $$
DECLARE
    menu_id INTEGER;
    assignment_id BIGINT;
BEGIN
    -- Insertar o actualizar menú (idempotente)
    menu_id := insert_menu_safe(
        16,                              -- parent_id_menu (NULL para menú raíz)
        'Nombre del Menú',               -- name
        '/ruta/del/menu',                -- url
        'icon_name',                     -- icon (Material Icons)
        'FEATURE_CODE'                   -- feature_code (opcional, puede ser NULL)
    );
    
    -- Asignar a super_admin (rol 1)
    assignment_id := assign_menu_to_role_safe(menu_id, 1);
    
    RAISE NOTICE '✅ Menu creado/actualizado: ID %, Assignment ID: %', menu_id, assignment_id;
END $$;

-- ============================================
-- Ejemplo 2: Crear múltiples menús
-- ============================================

DO $$
DECLARE
    parent_menu_id INTEGER;
    child_menu_id INTEGER;
BEGIN
    -- Crear menú padre
    parent_menu_id := insert_menu_safe(
        NULL,                           -- Sin padre (menú raíz)
        'Nuevo Grupo',
        NULL,                           -- Sin URL (solo contenedor)
        'group_icon',
        NULL                            -- Sin feature_code (menú padre)
    );
    
    -- Crear menú hijo
    child_menu_id := insert_menu_safe(
        parent_menu_id,                 -- Parent: el menú que acabamos de crear
        'Submenú',
        '/submenu',
        'submenu_icon',
        'SUBMENU_FEATURE_CODE'
    );
    
    -- Asignar ambos a super_admin
    PERFORM assign_menu_to_role_safe(parent_menu_id, 1);
    PERFORM assign_menu_to_role_safe(child_menu_id, 1);
    
    RAISE NOTICE '✅ Menús creados: Parent ID %, Child ID %', parent_menu_id, child_menu_id;
END $$;

-- ============================================
-- Ejemplo 3: Buscar menú existente y actualizarlo
-- ============================================

DO $$
DECLARE
    menu_id INTEGER;
BEGIN
    -- Buscar menú por feature_code
    menu_id := get_menu_id_by_feature_code('EXISTING_FEATURE_CODE');
    
    IF menu_id IS NOT NULL THEN
        -- Actualizar URL si existe
        UPDATE public.menu
        SET url = '/new-url'
        WHERE id_menu = menu_id;
        
        RAISE NOTICE '✅ Menú actualizado: ID %', menu_id;
    ELSE
        RAISE NOTICE '⚠️ Menú no encontrado';
    END IF;
END $$;

-- ============================================
-- Ejemplo 4: Asignar menú existente a múltiples roles
-- ============================================

DO $$
DECLARE
    menu_id INTEGER;
BEGIN
    -- Buscar menú por nombre
    menu_id := get_menu_id_by_name('Gestión de Permisos', 16);
    
    IF menu_id IS NOT NULL THEN
        -- Asignar a múltiples roles
        PERFORM assign_menu_to_role_safe(menu_id, 1);  -- Super Admin
        PERFORM assign_menu_to_role_safe(menu_id, 3);  -- Admin
        
        RAISE NOTICE '✅ Menú asignado a roles: ID %', menu_id;
    END IF;
END $$;

-- ============================================
-- VERIFICATION QUERIES (commented - run manually)
-- ============================================

-- Verificar menú creado
-- SELECT id_menu, name, url, feature_code, parent_id_menu
-- FROM public.menu
-- WHERE name = 'Nombre del Menú';

-- Verificar asignaciones
-- SELECT mr.id_menu_role, m.name, mr.id_role, r.name as role_name
-- FROM public.menu_role mr
-- INNER JOIN public.menu m ON m.id_menu = mr.id_menu
-- INNER JOIN public.role r ON r.id_role = mr.id_role
-- WHERE m.name = 'Nombre del Menú';
