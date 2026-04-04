-- ============================================
-- Script: Rename order column to menu_order
-- Date: 2026-01-01
-- Purpose: Rename 'order' column to 'menu_order' to avoid PostgreSQL reserved keyword conflicts
-- All existing values are NULL, so safe to rename
-- ============================================

-- ============================================
-- STEP 1: Rename column from 'order' to 'menu_order' if it exists
-- ============================================

DO $$
BEGIN
    -- Check if column 'order' exists
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'menu'
          AND column_name = 'order'
    ) THEN
        -- Rename column
        ALTER TABLE public.menu RENAME COLUMN "order" TO menu_order;
        RAISE NOTICE '✅ Column "order" renamed to menu_order';
    ELSE
        RAISE NOTICE '⚠️ Column "order" does not exist, skipping rename';
    END IF;
END $$;

-- ============================================
-- STEP 2: Rename index if it exists
-- ============================================

DO $$
BEGIN
    -- Check if index exists
    IF EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND tablename = 'menu'
          AND indexname = 'idx_menu_order'
    ) THEN
        -- Rename index
        ALTER INDEX public.idx_menu_order RENAME TO idx_menu_menu_order;
        RAISE NOTICE '✅ Index idx_menu_order renamed to idx_menu_menu_order';
    ELSE
        RAISE NOTICE '⚠️ Index idx_menu_order does not exist, skipping rename';
    END IF;
END $$;

-- ============================================
-- STEP 3: Update comment on column
-- ============================================

COMMENT ON COLUMN public.menu.menu_order IS 'Order/position of menu item for custom sorting. Lower numbers appear first.';
