-- ============================================
-- Script: Add sequence sync helper function
-- Date: 2026-01-21
-- Purpose: Create reusable helper function for synchronizing sequences with table max IDs
-- This function can be used in any migration after this one
-- ============================================

-- ============================================
-- SEQUENCE HELPER FUNCTIONS: Sequence synchronization (idempotent)
-- ============================================

-- FUNCTION: Sync sequence with table max ID
CREATE OR REPLACE FUNCTION sync_sequence_with_table(
    p_sequence_name VARCHAR,
    p_table_name VARCHAR,
    p_id_column_name VARCHAR,
    p_table_schema VARCHAR DEFAULT 'public'
)
RETURNS BIGINT AS $$
DECLARE
    max_id BIGINT;
    sql_statement TEXT;
    sequence_exists BOOLEAN;
BEGIN
    -- Check if sequence exists
    SELECT EXISTS (
        SELECT 1
        FROM pg_sequences
        WHERE schemaname = p_table_schema
          AND sequencename = p_sequence_name
    ) INTO sequence_exists;
    
    -- Create sequence if it doesn't exist
    IF NOT sequence_exists THEN
        sql_statement := format(
            'CREATE SEQUENCE IF NOT EXISTS %I.%I START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1',
            p_table_schema,
            p_sequence_name
        );
        EXECUTE sql_statement;
    END IF;
    
    -- Get the maximum ID from the table
    sql_statement := format(
        'SELECT COALESCE(MAX(%I), 0) FROM %I.%I',
        p_id_column_name,
        p_table_schema,
        p_table_name
    );
    EXECUTE sql_statement INTO max_id;
    
    -- Set the sequence to the max ID
    -- If max_id is 0 (empty table), use setval(seq, 1, false) so nextval() returns 1
    -- If max_id > 0, use setval(seq, max_id) so nextval() returns max_id + 1
    IF max_id = 0 THEN
        -- Table is empty: set sequence to 1 but mark as not called, so nextval() returns 1
        sql_statement := format(
            'SELECT setval(%L, 1, false)',
            p_table_schema || '.' || p_sequence_name
        );
    ELSE
        -- Table has data: set sequence to max_id, so nextval() returns max_id + 1
        sql_statement := format(
            'SELECT setval(%L, %s)',
            p_table_schema || '.' || p_sequence_name,
            max_id
        );
    END IF;
    EXECUTE sql_statement;
    
    RETURN max_id + 1; -- Return the next value that will be used
END;
$$ LANGUAGE plpgsql;

-- Add comment for documentation
COMMENT ON FUNCTION sync_sequence_with_table(VARCHAR, VARCHAR, VARCHAR, VARCHAR) IS 
'Generic helper function to create (if not exists) and synchronize a sequence with the maximum ID in a table. 
Parameters: sequence_name, table_name, id_column_name, table_schema (default: public).
Returns: The next value that will be used by the sequence.';

