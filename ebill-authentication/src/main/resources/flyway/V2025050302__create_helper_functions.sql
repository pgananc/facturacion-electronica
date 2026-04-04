-- ============================================
-- Script: Create Helper Functions
-- Date: 2025-05-03
-- Purpose: Create reusable helper functions for idempotent DDL operations and menu management
-- These functions can be used in any migration after this one
-- ============================================

-- ============================================
-- DDL HELPER FUNCTIONS: Generic DDL operations (idempotent)
-- ============================================

-- FUNCTION 1: Add column if not exists
CREATE OR REPLACE FUNCTION add_column_if_not_exists(
    p_table_name VARCHAR,
    p_column_name VARCHAR,
    p_data_type VARCHAR,
    p_table_schema VARCHAR DEFAULT 'public',
    p_length INTEGER DEFAULT NULL,
    p_not_null BOOLEAN DEFAULT FALSE,
    p_default_value TEXT DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
    column_exists BOOLEAN;
    sql_statement TEXT;
    full_type VARCHAR;
BEGIN
    -- Check if column already exists
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = p_table_schema
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) INTO column_exists;
    
    -- If column exists, return false (no change made)
    IF column_exists THEN
        RETURN FALSE;
    END IF;
    
    -- Build data type with optional length
    IF p_length IS NOT NULL THEN
        full_type := p_data_type || '(' || p_length || ')';
    ELSE
        full_type := p_data_type;
    END IF;
    
    -- Build ALTER TABLE statement
    sql_statement := format(
        'ALTER TABLE %I.%I ADD COLUMN %I %s',
        p_table_schema,
        p_table_name,
        p_column_name,
        full_type
    );
    
    -- Add DEFAULT if provided
    IF p_default_value IS NOT NULL THEN
        sql_statement := sql_statement || ' DEFAULT ' || p_default_value;
    END IF;
    
    -- Add NOT NULL if requested
    IF p_not_null THEN
        sql_statement := sql_statement || ' NOT NULL';
    END IF;
    
    EXECUTE sql_statement;
    
    RETURN TRUE; -- Column was added
END;
$$ LANGUAGE plpgsql;

-- FUNCTION 2: Add constraint if not exists
CREATE OR REPLACE FUNCTION add_constraint_if_not_exists(
    p_table_name VARCHAR,
    p_constraint_name VARCHAR,
    p_constraint_type VARCHAR, -- 'FOREIGN KEY', 'UNIQUE', 'CHECK', 'PRIMARY KEY'
    p_constraint_definition TEXT,
    p_table_schema VARCHAR DEFAULT 'public'
)
RETURNS BOOLEAN AS $$
DECLARE
    constraint_exists BOOLEAN;
    sql_statement TEXT;
BEGIN
    -- Check if constraint already exists
    SELECT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = p_table_schema
          AND table_name = p_table_name
          AND constraint_name = p_constraint_name
    ) INTO constraint_exists;
    
    -- If constraint exists, return false (no change made)
    IF constraint_exists THEN
        RETURN FALSE;
    END IF;
    
    -- Build ALTER TABLE statement
    -- Note: p_constraint_type can be 'FOREIGN KEY', 'UNIQUE', 'CHECK', etc.
    -- p_constraint_definition contains the actual constraint definition
    sql_statement := format(
        'ALTER TABLE %I.%I ADD CONSTRAINT %I %s %s',
        p_table_schema,
        p_table_name,
        p_constraint_name,
        p_constraint_type,
        p_constraint_definition
    );
    
    -- Execute with error handling
    BEGIN
        EXECUTE sql_statement;
    EXCEPTION
        WHEN OTHERS THEN
            -- If constraint creation fails, log and re-raise
            RAISE WARNING 'Failed to create constraint % on table %.%: %', 
                p_constraint_name, p_table_schema, p_table_name, SQLERRM;
            RAISE;
    END;
    
    RETURN TRUE; -- Constraint was added
END;
$$ LANGUAGE plpgsql;

-- FUNCTION 3: Create index if not exists
CREATE OR REPLACE FUNCTION create_index_if_not_exists(
    p_index_name VARCHAR,
    p_table_name VARCHAR,
    p_columns TEXT, -- Can be single column or comma-separated: 'column1' or 'column1, column2'
    p_table_schema VARCHAR DEFAULT 'public',
    p_unique BOOLEAN DEFAULT FALSE,
    p_where_clause TEXT DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
    index_exists BOOLEAN;
    sql_statement TEXT;
    unique_keyword TEXT;
BEGIN
    -- Check if index already exists
    SELECT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = p_table_schema
          AND tablename = p_table_name
          AND indexname = p_index_name
    ) INTO index_exists;
    
    -- If index exists, return false (no change made)
    IF index_exists THEN
        RETURN FALSE;
    END IF;
    
    -- Build CREATE INDEX statement
    unique_keyword := CASE WHEN p_unique THEN 'UNIQUE' ELSE '' END;
    
    sql_statement := format(
        'CREATE %s INDEX %I ON %I.%I (%s)',
        unique_keyword,
        p_index_name,
        p_table_schema,
        p_table_name,
        p_columns
    );
    
    -- Add WHERE clause if provided (partial index)
    IF p_where_clause IS NOT NULL THEN
        sql_statement := sql_statement || ' WHERE ' || p_where_clause;
    END IF;
    
    EXECUTE sql_statement;
    
    RETURN TRUE; -- Index was created
END;
$$ LANGUAGE plpgsql;

-- FUNCTION 4: Add comment to column if not exists
CREATE OR REPLACE FUNCTION add_column_comment_if_not_exists(
    p_table_name VARCHAR,
    p_column_name VARCHAR,
    p_comment TEXT,
    p_table_schema VARCHAR DEFAULT 'public'
)
RETURNS BOOLEAN AS $$
DECLARE
    comment_exists BOOLEAN;
    sql_statement TEXT;
BEGIN
    -- Check if comment already exists
    SELECT EXISTS (
        SELECT 1
        FROM pg_catalog.pg_description d
        JOIN pg_catalog.pg_class c ON d.objoid = c.oid
        JOIN pg_catalog.pg_namespace n ON c.relnamespace = n.oid
        JOIN pg_catalog.pg_attribute a ON d.objoid = a.attrelid AND d.objsubid = a.attnum
        WHERE n.nspname = p_table_schema
          AND c.relname = p_table_name
          AND a.attname = p_column_name
          AND d.description = p_comment
    ) INTO comment_exists;
    
    -- If comment exists with same text, return false
    IF comment_exists THEN
        RETURN FALSE;
    END IF;
    
    -- Build COMMENT statement (always updates, even if different comment exists)
    sql_statement := format(
        'COMMENT ON COLUMN %I.%I.%I IS %L',
        p_table_schema,
        p_table_name,
        p_column_name,
        p_comment
    );
    
    EXECUTE sql_statement;
    
    RETURN TRUE; -- Comment was added/updated
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- MENU HELPER FUNCTIONS: Menu and menu_role management (idempotent)
-- ============================================

-- FUNCTION 5: Get next menu ID safely
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

-- FUNCTION 6: Get next menu_role ID safely
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

-- FUNCTION 7: Get or find menu ID by name/parent
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

-- FUNCTION 8: Get or find menu ID by feature_code
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

-- FUNCTION 9: Safely insert menu and return ID
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

-- FUNCTION 10: Safely assign menu to role
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

