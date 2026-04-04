-- ============================================
-- Script: Add order column to menu table
-- Date: 2025-12-31
-- Purpose: Add order field to menu table for custom menu ordering
-- Note: Helper functions are defined in V2025050302__create_helper_functions.sql
-- ============================================

-- Add order column to menu table using helper function (idempotent)
SELECT add_column_if_not_exists(
    'menu'::VARCHAR,             -- table name
    'order'::VARCHAR,            -- column name
    'INTEGER'::VARCHAR,          -- data type
    'public'::VARCHAR,           -- schema
    NULL::INTEGER                -- length (not needed for INTEGER)
);

-- Create index for better query performance when ordering menus (idempotent)
SELECT create_index_if_not_exists(
    'idx_menu_order'::VARCHAR,   -- index name
    'menu'::VARCHAR,             -- table name
    '"order"'::TEXT,          -- columns (quoted because 'order' is a reserved keyword)
    'public'::VARCHAR            -- schema
);

-- Add comment for documentation (idempotent)
SELECT add_column_comment_if_not_exists(
    'menu'::VARCHAR,                                                              -- table name
    'order'::VARCHAR,                                                             -- column name
    'Order/position of menu item for custom sorting. Lower numbers appear first.'::TEXT, -- comment
    'public'::VARCHAR                                                             -- schema
);
