-- =====================================================
-- Migration: Separar IVA e ICE en Product
-- Fecha: 2025-11-19
-- Descripción: Renombrar id_tax_type a id_iva_tax_type
--              y agregar id_ice_tax_type para permitir
--              productos con IVA + ICE simultáneamente
-- =====================================================
-- Paso 1: Renombrar columna existente a id_iva_tax_type
ALTER TABLE public.product
    RENAME COLUMN id_tax_type TO id_iva_tax_type;
-- Paso 2: Agregar comentario explicativo a la columna renombrada
COMMENT ON COLUMN public.product.id_iva_tax_type IS 'FK a tax_type con type=IVA (obligatorio)';
-- Paso 3: Agregar nueva columna para ICE (nullable porque es opcional)
ALTER TABLE public.product
ADD COLUMN id_ice_tax_type BIGINT;
-- Paso 4: Agregar comentario a la nueva columna
COMMENT ON COLUMN public.product.id_ice_tax_type IS 'FK a tax_type con type=ICE (opcional)';
-- Paso 5: Crear índice para la nueva columna (mejor performance en JOINs)
CREATE INDEX idx_product_ice_tax_type ON public.product(id_ice_tax_type);
-- Paso 6: Agregar constraint de clave foránea para ICE
ALTER TABLE public.product
ADD CONSTRAINT fk_product_ice_tax_type FOREIGN KEY (id_ice_tax_type) REFERENCES public.tax_type(id_tax_type);
-- Paso 7: Actualizar comentario de has_ice para claridad
COMMENT ON COLUMN public.product.has_ice IS 'Flag UI: true=muestra combo ICE, false=oculta combo ICE';
-- Paso 8: Actualizar comentario de has_tourism_iva
COMMENT ON COLUMN public.product.has_tourism_iva IS 'Flag especial: IVA reducido en feriados (dinamización económica)';
-- =====================================================
-- Validaciones de integridad (opcional pero recomendado)
-- =====================================================
-- Si quieres asegurar que id_ice_tax_type solo tenga types con 'ICE':
-- ALTER TABLE public.product
--     ADD CONSTRAINT chk_ice_tax_type_is_ice
--         CHECK (
--             id_ice_tax_type IS NULL OR
--             EXISTS (
--                 SELECT 1 FROM tax_type
--                 WHERE id_tax_type = id_ice_tax_type
--                 AND type = 'ICE'
--             )
--         );
-- Verificación final
DO $$ BEGIN RAISE NOTICE 'Migración completada exitosamente';
RAISE NOTICE '- Columna renombrada: id_tax_type → id_iva_tax_type';
RAISE NOTICE '- Nueva columna agregada: id_ice_tax_type';
RAISE NOTICE '- Constraints e índices creados';
END $$;