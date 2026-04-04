-- Actualizar URLs de los submenús del menú "Compañía" (ID: 3)
-- Agregar prefijo del padre (/company/) a las URLs de los hijos

-- Actualizar URL de "Gestionar Sucursal" (ID: 14) de '/establishment' a '/company/establishment'
UPDATE public.menu
SET url = '/company/establishment'
WHERE id_menu = 14
  AND parent_id_menu = 3
  AND name = 'Gestionar Sucursal';

-- Actualizar URL de "Gestionar Punto de Emisión" (ID: 15) de '/invoice-point' a '/company/invoice-point'
UPDATE public.menu
SET url = '/company/invoice-point'
WHERE id_menu = 15
  AND parent_id_menu = 3
  AND name = 'Gestionar Punto de Emisión';

