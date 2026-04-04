-- Actualizar URL del menú "Gestionar usuarios por compañía"
-- Cambiar de /company-user a /company/company-user
UPDATE public.menu
SET url = '/company/company-user'
WHERE id_menu = 7
  AND parent_id_menu = 4
  AND name = 'Gestionar usuarios por compañía';

