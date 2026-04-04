-- ============================================
-- Script: Sync all sequences with their tables
-- Date: 2026-01-22
-- Purpose: Synchronize all sequences with their corresponding table max IDs
-- This fixes "duplicate key value violates unique constraint" errors
-- Uses helper function sync_sequence_with_table() from V2026012200__add_sequence_sync_helper_function.sql
-- ============================================

-- Synchronize all sequences with their tables using helper function
-- This will create sequences if they don't exist and sync them with max IDs

-- 1. Users
SELECT sync_sequence_with_table('user_seq'::VARCHAR, 'users'::VARCHAR, 'id_user'::VARCHAR, 'public'::VARCHAR);

-- 2. Menu
SELECT sync_sequence_with_table('menu_seq'::VARCHAR, 'menu'::VARCHAR, 'id_menu'::VARCHAR, 'public'::VARCHAR);

-- 3. Menu Role
SELECT sync_sequence_with_table('menu_role_seq'::VARCHAR, 'menu_role'::VARCHAR, 'id_menu_role'::VARCHAR, 'public'::VARCHAR);

-- 4. Role
SELECT sync_sequence_with_table('role_seq'::VARCHAR, 'role'::VARCHAR, 'id_role'::VARCHAR, 'public'::VARCHAR);

-- 5. Params
SELECT sync_sequence_with_table('params_seq'::VARCHAR, 'params'::VARCHAR, 'id_param'::VARCHAR, 'public'::VARCHAR);

-- 6. Token
SELECT sync_sequence_with_table('token_seq'::VARCHAR, 'token'::VARCHAR, 'id_token'::VARCHAR, 'public'::VARCHAR);

-- 7. Client Token
SELECT sync_sequence_with_table('client_token_seq'::VARCHAR, 'client_token'::VARCHAR, 'id_client_token'::VARCHAR, 'public'::VARCHAR);

-- 8. Feature Permission
SELECT sync_sequence_with_table('feature_permission_seq'::VARCHAR, 'feature_permission'::VARCHAR, 'id_permission'::VARCHAR, 'public'::VARCHAR);

-- 9. User Company
SELECT sync_sequence_with_table('user_company_seq'::VARCHAR, 'user_company'::VARCHAR, 'id_user_company'::VARCHAR, 'public'::VARCHAR);

-- 10. User Company Role
SELECT sync_sequence_with_table('user_company_role_seq'::VARCHAR, 'user_company_role'::VARCHAR, 'id_user_company_role'::VARCHAR, 'public'::VARCHAR);

-- 11. User Invoice Point Session
SELECT sync_sequence_with_table('user_invoice_point_session_seq'::VARCHAR, 'user_invoice_point_session'::VARCHAR, 'id_user_invoice_point_session'::VARCHAR, 'public'::VARCHAR);

-- 12. User Company Establishment
SELECT sync_sequence_with_table('user_company_establishment_seq'::VARCHAR, 'user_company_establishment'::VARCHAR, 'id_user_company_establishment'::VARCHAR, 'public'::VARCHAR);

