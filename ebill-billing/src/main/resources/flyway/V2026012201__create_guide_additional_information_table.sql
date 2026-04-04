-- =====================================================
-- GUÍAS DE REMISIÓN - Tabla de Información Adicional
-- =====================================================

-- 1. Crear secuencia
CREATE SEQUENCE IF NOT EXISTS guide_additional_information_seq START WITH 1 INCREMENT BY 1;

-- 2. Crear tabla guide_additional_information
CREATE TABLE guide_additional_information (
    id_guide_additional_information BIGINT NOT NULL PRIMARY KEY,
    id_guide BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guide_additional_information_guide 
        FOREIGN KEY (id_guide) REFERENCES guide(id_guide) ON DELETE CASCADE
);

-- 3. Crear índices
CREATE INDEX idx_guide_additional_information_guide ON guide_additional_information(id_guide);

-- 4. Comentarios
COMMENT ON TABLE guide_additional_information IS 'Tabla de información adicional de guías de remisión (campos adicionales para SRI)';
COMMENT ON COLUMN guide_additional_information.id_guide IS 'FK a la guía de remisión';
COMMENT ON COLUMN guide_additional_information.name IS 'Nombre del campo adicional (ej: fono, dir)';
COMMENT ON COLUMN guide_additional_information.description IS 'Valor/descripción del campo adicional';


