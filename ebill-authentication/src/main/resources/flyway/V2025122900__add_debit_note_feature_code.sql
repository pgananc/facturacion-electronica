-- ============================================
-- Script: Add feature_code to Debit Note menu
-- Date: 2025-12-29
-- Purpose: Assign DEBIT_NOTE_CREATE feature_code to "Nota de débito" menu
-- Uses helper functions for safe, idempotent execution
-- ============================================

-- ============================================
-- STEP 1: Create DEBIT_NOTE_CREATE feature if not exists
-- ============================================

INSERT INTO public.feature_permission (
    feature_code,
    feature_name,
    description,
    module,
    allowed_roles,
    required_plans,
    is_active
)
VALUES (
    'DEBIT_NOTE_CREATE',
    'Crear Notas de Débito',
    'Crear y emitir notas de débito',
    'CREDIT_NOTES', -- Same module as credit notes
    ARRAY[1, 2, 3], -- Same roles as CREDIT_NOTE_CREATE (super_admin, member, admin)
    ARRAY['BASIC', 'PROFESSIONAL', 'ENTERPRISE'], -- Same plans as CREDIT_NOTE_CREATE
    TRUE
)
ON CONFLICT (feature_code) DO NOTHING;

-- ============================================
-- STEP 2: Update "Nota de débito" menu to assign feature_code
-- Uses helper function get_menu_id_by_name() for safe lookup
-- ============================================

DO $$
DECLARE
    debit_note_menu_id INTEGER;
BEGIN
    -- Find menu by name and parent (parent_id = 5 is "Facturación Electrónica")
    debit_note_menu_id := get_menu_id_by_name('Nota de débito', 5);
    
    IF debit_note_menu_id IS NOT NULL THEN
        UPDATE public.menu
        SET feature_code = 'DEBIT_NOTE_CREATE'
        WHERE id_menu = debit_note_menu_id
          AND (feature_code IS NULL OR feature_code != 'DEBIT_NOTE_CREATE');
        
        RAISE NOTICE '✅ Menu "Nota de débito" (ID: %) updated with feature_code DEBIT_NOTE_CREATE', debit_note_menu_id;
    ELSE
        RAISE WARNING '⚠️ Menu "Nota de débito" not found with parent_id = 5';
    END IF;
END $$;

-- ============================================
-- STEP 3: Verification queries (commented - run manually if needed)
-- ============================================

-- Verify feature was created
-- SELECT feature_code, feature_name, module, allowed_roles, required_plans
-- FROM public.feature_permission
-- WHERE feature_code = 'DEBIT_NOTE_CREATE';

-- Verify menu was updated
-- SELECT id_menu, name, url, feature_code
-- FROM public.menu
-- WHERE name = 'Nota de débito';

-- Expected result: id_menu=34, name='Nota de débito', url='/debit-note', feature_code='DEBIT_NOTE_CREATE'
