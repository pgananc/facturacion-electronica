-- ============================================
-- Script: Create menus for administrative features
-- Date: 2025-12-18
-- Purpose: Create menu items for ROLE_PERMISSION_MANAGEMENT and SUBSCRIPTION_MANAGEMENT
-- These menus are for super_admin only
-- Uses helper functions for safe, idempotent execution
-- ============================================

-- ============================================
-- STEP 1: Create helper functions (if not exist)
-- These functions are reusable across all migrations
-- ============================================

-- FUNCTION: Get next menu ID safely
CREATE OR REPLACE FUNCTION get_next_menu_id()
RETURNS INTEGER AS $$
DECLARE
    max_id INTEGER;
BEGIN
    SELECT COALESCE(MAX(id_menu), 0) INTO max_id FROM public.menu;
    PERFORM setval('menu_seq', max_id);
    RETURN nextval('menu_seq');
END;
$$ LANGUAGE plpgsql;

-- FUNCTION: Get next menu_role ID safely
CREATE OR REPLACE FUNCTION get_next_menu_role_id()
RETURNS BIGINT AS $$
DECLARE
    max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id_menu_role), 0) INTO max_id FROM public.menu_role;
    PERFORM setval('menu_role_seq', max_id);
    RETURN nextval('menu_role_seq');
END;
$$ LANGUAGE plpgsql;

-- FUNCTION: Get or find menu ID by name/parent
CREATE OR REPLACE FUNCTION get_menu_id_by_name(
    p_name VARCHAR,
    p_parent_id INTEGER DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE
    found_id INTEGER;
BEGIN
    SELECT id_menu INTO found_id
    FROM public.menu
    WHERE name = p_name
      AND (p_parent_id IS NULL OR parent_id_menu = p_parent_id)
      AND status = true
    LIMIT 1;
    RETURN found_id;
END;
$$ LANGUAGE plpgsql;

-- FUNCTION: Get or find menu ID by feature_code
CREATE OR REPLACE FUNCTION get_menu_id_by_feature_code(
    p_feature_code VARCHAR
)
RETURNS INTEGER AS $$
DECLARE
    found_id INTEGER;
BEGIN
    SELECT id_menu INTO found_id
    FROM public.menu
    WHERE feature_code = p_feature_code
      AND status = true
    LIMIT 1;
    RETURN found_id;
END;
$$ LANGUAGE plpgsql;

-- FUNCTION: Safely insert menu and return ID
CREATE OR REPLACE FUNCTION insert_menu_safe(
    p_parent_id INTEGER,
    p_name VARCHAR,
    p_url VARCHAR,
    p_icon VARCHAR,
    p_feature_code VARCHAR DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE
    existing_id INTEGER;
    new_id INTEGER;
BEGIN
    existing_id := get_menu_id_by_name(p_name, p_parent_id);
    
    IF existing_id IS NOT NULL THEN
        UPDATE public.menu
        SET url = p_url,
            icon = p_icon,
            feature_code = COALESCE(p_feature_code, feature_code),
            status = true
        WHERE id_menu = existing_id;
        RETURN existing_id;
    END IF;
    
    new_id := get_next_menu_id();
    
    INSERT INTO public.menu (
        id_menu, parent_id_menu, name, url, icon, status, feature_code
    ) VALUES (
        new_id, p_parent_id, p_name, p_url, p_icon, true, p_feature_code
    );
    
    RETURN new_id;
END;
$$ LANGUAGE plpgsql;

-- FUNCTION: Safely assign menu to role
CREATE OR REPLACE FUNCTION assign_menu_to_role_safe(
    p_menu_id INTEGER,
    p_role_id INTEGER
)
RETURNS BIGINT AS $$
DECLARE
    existing_id BIGINT;
    new_id BIGINT;
BEGIN
    SELECT id_menu_role INTO existing_id
    FROM public.menu_role
    WHERE id_menu = p_menu_id
      AND id_role = p_role_id
      AND status = true
    LIMIT 1;
    
    IF existing_id IS NOT NULL THEN
        UPDATE public.menu_role SET status = true WHERE id_menu_role = existing_id;
        RETURN existing_id;
    END IF;
    
    new_id := get_next_menu_role_id();
    
    INSERT INTO public.menu_role (
        id_menu_role, id_menu, id_role, status
    ) VALUES (
        new_id, p_menu_id, p_role_id, true
    );
    
    RETURN new_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- STEP 2: Create menus using helper functions
-- ============================================

-- ============================================
-- MENU: Gestión de Permisos (Role Permission Management)
-- Parent: Administración Global (ID: 16)
-- ============================================

DO $$
DECLARE
    menu_id INTEGER;
BEGIN
    menu_id := insert_menu_safe(
        16,                              -- parent_id_menu: Administración Global
        'Gestión de Permisos',          -- name
        '/admin/role-permissions',       -- url (updated to match V2025121708)
        'security',                     -- icon (updated)
        'ROLE_PERMISSION_MANAGEMENT'     -- feature_code
    );
    
    RAISE NOTICE '✅ Menu "Gestión de Permisos" - ID: %', menu_id;
END $$;

-- ============================================
-- MENU: Gestión de Suscripciones (Subscription Management)
-- Parent: Administración Global (ID: 16)
-- ============================================

DO $$
DECLARE
    menu_id INTEGER;
BEGIN
    menu_id := insert_menu_safe(
        16,                              -- parent_id_menu: Administración Global
        'Gestión de Suscripciones',     -- name
        '/admin/subscriptions',          -- url (updated to match V2025121708)
        'card_membership',               -- icon
        'SUBSCRIPTION_MANAGEMENT'        -- feature_code
    );
    
    RAISE NOTICE '✅ Menu "Gestión de Suscripciones" - ID: %', menu_id;
END $$;

-- ============================================
-- VERIFICATION QUERIES (commented - run manually)
-- ============================================

-- Verify menus were created
-- SELECT id_menu, parent_id_menu, name, url, feature_code 
-- FROM public.menu 
-- WHERE id_menu IN (32, 33);

-- Expected output:
-- 32 | 16 | Gestión de Permisos | /permissions | ROLE_PERMISSION_MANAGEMENT
-- 33 | 16 | Gestión de Suscripciones | /subscriptions | SUBSCRIPTION_MANAGEMENT

-- Verify parent menu exists (Administración Global)
-- SELECT id_menu, name FROM public.menu WHERE id_menu = 16;
