-- ============================================
-- Script: Create menu_role_seq sequence
-- Date: 2025-12-17
-- Purpose: Create sequence for menu_role table primary key generation
-- This sequence must be created before V2025121702 which uses it
-- ============================================

-- Create sequence for menu_role table
CREATE SEQUENCE IF NOT EXISTS public.menu_role_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Add comment for documentation
COMMENT ON SEQUENCE public.menu_role_seq IS 'Sequence for generating primary keys for menu_role table';


-- Create sequence for menu_role table
CREATE SEQUENCE IF NOT EXISTS public.menu_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Add comment for documentation
COMMENT ON SEQUENCE public.menu_seq IS 'Sequence for generating primary keys for menu table';

