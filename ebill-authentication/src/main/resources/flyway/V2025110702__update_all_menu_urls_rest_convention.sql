-- Actualizar todas las URLs de menús para seguir convención REST estándar
-- Reglas aplicadas:
-- 1. Recursos en plural: /companies, /users, /clients, /products, /invoices
-- 2. Sub-recursos con nombres directos (sin prefijo del padre): /establishments, /invoice-points, /drafts, /company-users
-- 3. Consistencia en nomenclatura (kebab-case para múltiples palabras)

-- Actualizar "Gestionar compañía" (ID: 10) de '/company' a '/companies'
UPDATE public.menu
SET url = '/companies'
WHERE id_menu = 10
  AND parent_id_menu = 3
  AND name = 'Gestionar compañía';

-- Actualizar "Gestionar Sucursal" (ID: 14) de '/company/establishment' a '/establishments'
UPDATE public.menu
SET url = '/establishments'
WHERE id_menu = 14
  AND parent_id_menu = 3
  AND name = 'Gestionar Sucursal';

-- Actualizar "Gestionar Punto de Emisión" (ID: 15) de '/company/invoice-point' a '/invoice-points'
UPDATE public.menu
SET url = '/invoice-points'
WHERE id_menu = 15
  AND parent_id_menu = 3
  AND name = 'Gestionar Punto de Emisión';

-- Actualizar "Gestionar clientes" (ID: 8) de '/client' a '/clients'
UPDATE public.menu
SET url = '/clients'
WHERE id_menu = 8
  AND parent_id_menu = 1
  AND name = 'Gestionar clientes';

-- Actualizar "Gestionar productos" (ID: 9) de '/product' a '/products'
UPDATE public.menu
SET url = '/products'
WHERE id_menu = 9
  AND parent_id_menu = 2
  AND name = 'Gestionar productos';

-- Actualizar "Gestionar usuario" (ID: 11) de '/user' a '/users'
UPDATE public.menu
SET url = '/users'
WHERE id_menu = 11
  AND parent_id_menu = 4
  AND name = 'Gestionar usuario';

-- Actualizar "Gestionar usuarios por compañía" (ID: 7) de '/company/company-user' a '/company-users'
UPDATE public.menu
SET url = '/company-users'
WHERE id_menu = 7
  AND parent_id_menu = 4
  AND name = 'Gestionar usuarios por compañía';

-- Actualizar "Facturas" (ID: 6) de '/billing' a '/invoices'
UPDATE public.menu
SET url = '/invoices'
WHERE id_menu = 6
  AND parent_id_menu = 5
  AND name = 'Facturas';

-- Actualizar "Borradores" (ID: 12) de '/drafts' a '/drafts' (ya está correcto, solo asegurar plural)
UPDATE public.menu
SET url = '/drafts'
WHERE id_menu = 12
  AND parent_id_menu = 5
  AND name = 'Borradores';

