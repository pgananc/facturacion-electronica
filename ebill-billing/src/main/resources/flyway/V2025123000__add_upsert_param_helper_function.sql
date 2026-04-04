-- =====================================================
-- HELPER FUNCTION: upsert_param
-- Fecha: 2025-12-30
-- Descripción: Función helper para inserción/actualización segura de parámetros (UPSERT)
--              que evita conflictos en despliegues concurrentes
-- =====================================================
-- NOTA: Esta función se ejecuta antes de V2025123001 que la utiliza
-- =====================================================
-- FUNCTION: upsert_param
-- Inserta un parámetro si no existe, o lo actualiza si ya existe (UPSERT)
-- =====================================================
CREATE OR REPLACE FUNCTION upsert_param(
        p_code_param VARCHAR(255),
        p_value_param TEXT,
        p_description_param VARCHAR(255)
    ) RETURNS VOID AS $$
DECLARE v_next_id BIGINT;
BEGIN -- Obtener el siguiente ID disponible (solo si no existe)
SELECT COALESCE(MAX(id_param), 0) + 1 INTO v_next_id
FROM params;
-- UPSERT: Insertar si no existe, actualizar si existe
INSERT INTO params (
        id_param,
        code_param,
        value_param,
        description_param,
        status,
        creation_date
    )
VALUES (
        v_next_id,
        p_code_param,
        p_value_param,
        p_description_param,
        TRUE,
        CURRENT_TIMESTAMP
    ) ON CONFLICT (code_param) DO
UPDATE
SET value_param = EXCLUDED.value_param,
    description_param = EXCLUDED.description_param,
    update_date = CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;
COMMENT ON FUNCTION upsert_param IS 'Inserta un parámetro si no existe, o lo actualiza si ya existe (UPSERT). Calcula automáticamente el siguiente ID disponible si es nuevo. Evita conflictos en despliegues concurrentes.';