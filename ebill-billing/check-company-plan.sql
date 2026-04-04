-- =====================================================
-- VALIDAR PLAN DE SUSCRIPCIÓN DE UNA EMPRESA
-- =====================================================
-- 1. Ver plan activo de una empresa específica (cambiar id_company)
SELECT cs.id_company,
  cs.subscription_status,
  sp.code as plan_code,
  sp.name as plan_name,
  sp.document_limit_yearly,
  cs.current_year_usage,
  (sp.document_limit_yearly - cs.current_year_usage) as remaining_documents,
  ROUND(
    (
      cs.current_year_usage * 100.0 / sp.document_limit_yearly
    ),
    2
  ) as percent_used,
  cs.start_date,
  cs.expires_at,
  CASE
    WHEN cs.expires_at < CURRENT_TIMESTAMP THEN 'EXPIRADA'
    WHEN cs.current_year_usage >= sp.document_limit_yearly THEN 'LÍMITE ALCANZADO'
    ELSE 'ACTIVA'
  END as status_detail
FROM company_subscription cs
  INNER JOIN subscription_plan sp ON cs.id_plan = sp.id_plan
WHERE cs.id_company = 1 -- CAMBIAR ESTE ID
  AND cs.subscription_status = 'ACTIVE';
-- 2. Ver todas las empresas con sus planes
SELECT cs.id_company,
  sp.code as plan_code,
  sp.name as plan_name,
  cs.current_year_usage as used,
  sp.document_limit_yearly as
limit, (sp.document_limit_yearly - cs.current_year_usage) as remaining,
  cs.subscription_status,
  cs.expires_at
FROM company_subscription cs
  INNER JOIN subscription_plan sp ON cs.id_plan = sp.id_plan
WHERE cs.subscription_status = 'ACTIVE'
ORDER BY cs.id_company;
-- 3. Ver empresas que están cerca del límite (más del 80% usado)
SELECT cs.id_company,
  sp.code as plan_code,
  sp.name as plan_name,
  cs.current_year_usage,
  sp.document_limit_yearly,
  ROUND(
    (
      cs.current_year_usage * 100.0 / sp.document_limit_yearly
    ),
    2
  ) as percent_used
FROM company_subscription cs
  INNER JOIN subscription_plan sp ON cs.id_plan = sp.id_plan
WHERE cs.subscription_status = 'ACTIVE'
  AND (
    cs.current_year_usage * 100.0 / sp.document_limit_yearly
  ) > 80
ORDER BY percent_used DESC;
-- 4. Ver features disponibles para una empresa
SELECT cs.id_company,
  sp.code as plan_code,
  sp.name as plan_name,
  sp.has_electronic_invoice,
  sp.has_credit_note,
  sp.has_debit_note,
  sp.has_retention,
  sp.has_guide,
  sp.has_api_access,
  sp.has_multi_user,
  sp.user_limit,
  sp.establishment_limit,
  sp.issuance_point_limit
FROM company_subscription cs
  INNER JOIN subscription_plan sp ON cs.id_plan = sp.id_plan
WHERE cs.id_company = 1 -- CAMBIAR ESTE ID
  AND cs.subscription_status = 'ACTIVE';
-- 5. Historial de uso mensual de una empresa
SELECT month_year,
  invoices_count,
  credit_notes_count,
  debit_notes_count,
  total_documents
FROM subscription_usage_history
WHERE id_company = 1 -- CAMBIAR ESTE ID
ORDER BY month_year DESC
LIMIT 12;