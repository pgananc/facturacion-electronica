-- Actualizar URLs de los submenús principales de "Reportes" que estaban en NULL

-- Actualizar URL de "Reportes operativos" (ID: 21)
UPDATE public.menu
SET url = '/reports/operational'
WHERE id_menu = 21 AND url IS NULL;

-- Actualizar URL de "Reporte Novedades" (ID: 26)
UPDATE public.menu
SET url = '/reports/news'
WHERE id_menu = 26 AND url IS NULL;

-- Actualizar URL de "Reportes comerciales" (ID: 28)
UPDATE public.menu
SET url = '/reports/sales'
WHERE id_menu = 28 AND url IS NULL;

