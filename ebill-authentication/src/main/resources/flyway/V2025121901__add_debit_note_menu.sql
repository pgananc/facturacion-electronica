INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
SELECT 
    (SELECT COALESCE(MAX(id_menu), 0) + 1 FROM public.menu), -- Calcula el ID máximo + 1
    5,                                                      -- Mismo padre que Nota de Crédito
    'payments',                                             -- Icono representativo de cargo/pago
    'Nota de débito',                                       -- Nombre del menú
    '/debit-note',                                          -- URL en inglés (buena práctica)
    true                                                    -- Estado activo
WHERE NOT EXISTS (SELECT 1 FROM public.menu WHERE name = 'Nota de débito');

INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
SELECT 
    (SELECT COALESCE(MAX(id_menu_role), 0) + 1 FROM public.menu_role), -- Siguiente ID
    (SELECT id_menu FROM public.menu WHERE name = 'Nota de débito' LIMIT 1), -- Busca el ID del menú creado
    1,                                                                  -- ID del Rol (Admin)
    true
WHERE NOT EXISTS (
    SELECT 1 FROM public.menu_role 
    WHERE id_menu = (SELECT id_menu FROM public.menu WHERE name = 'Nota de débito' LIMIT 1)
    AND id_role = 1
);

