INSERT INTO public.params (
        id_param,
        code_param,
        value_param,
        description_param,
        status,
        creation_date
    )
SELECT 4,
    'SUMMARY_TOTALS_LABELS',
    'SUBTOTAL 15%|SUBTOTAL 5%|SUBTOTAL 0%|SUBTOTAL SIN IMPUESTOS|DESCUENTO|ICE|IVA 15%|IVA 5%|IVA 0%|Propina 10%|Valor a pagar',
    'Etiquetas dinámicas para el bloque de totales del RIDE',
    TRUE,
    NOW()
WHERE NOT EXISTS (
        SELECT 1
        FROM public.params
        WHERE code_param = 'SUMMARY_TOTALS_LABELS'
    );
UPDATE public.params
SET value_param = 'SUBTOTAL 15%|SUBTOTAL 5%|SUBTOTAL 0%|SUBTOTAL SIN IMPUESTOS|DESCUENTO|ICE|IVA 15%|IVA 5%|IVA 0%|Propina 10%|Valor a pagar',
    description_param = 'Etiquetas dinámicas para el bloque de totales del RIDE'
WHERE code_param = 'SUMMARY_TOTALS_LABELS';