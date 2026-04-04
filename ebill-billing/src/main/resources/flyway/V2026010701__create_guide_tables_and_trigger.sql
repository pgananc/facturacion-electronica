-- =====================================================
-- GUÍAS DE REMISIÓN - Tablas y Trigger
-- =====================================================

-- 1. Crear secuencias
CREATE SEQUENCE IF NOT EXISTS guide_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS guide_detail_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS guide_document_reference_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS carrier_seq START WITH 1 INCREMENT BY 1;

-- 2. Crear tabla carrier (transportistas)
CREATE TABLE carrier (
    id_carrier BIGINT NOT NULL PRIMARY KEY,
    id_company BIGINT NOT NULL,
    identification_type VARCHAR(2) NOT NULL,
    identification VARCHAR(13) NOT NULL,
    name VARCHAR(300) NOT NULL,
    address VARCHAR(300),
    phone VARCHAR(20),
    email VARCHAR(100),
    license_number VARCHAR(50),
    vehicle_type VARCHAR(50),
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_carrier_company_identification 
        UNIQUE (id_company, identification)
);

-- 3. Crear tabla guide (guías de remisión)
CREATE TABLE guide (
    id_guide BIGINT NOT NULL PRIMARY KEY,
    -- Campos comunes
    id_company BIGINT NOT NULL,
    id_client BIGINT,
    id_establishment BIGINT,
    id_issuance_point BIGINT,
    sequential BIGINT,
    retry_count INTEGER DEFAULT 0,
    issue_date TIMESTAMP NOT NULL,
    guide_number VARCHAR(17),
    environment VARCHAR(10),
    additional_information TEXT,
    email_sent BOOLEAN DEFAULT FALSE,
    email_sent_date TIMESTAMP,
    id_authorization_sri BIGINT,
    -- Campos específicos de guía
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    transport_type VARCHAR(50),
    transport_plate VARCHAR(20),
    id_carrier BIGINT NOT NULL,
    origin_address VARCHAR(300) NOT NULL,
    destination_address VARCHAR(300) NOT NULL,
    reason_for_transfer VARCHAR(300),
    route VARCHAR(300),
    -- Campos BaseEntity
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT guide_authorization_sri_id_authorization_sri_fk 
        FOREIGN KEY (id_authorization_sri) REFERENCES authorization_sri(id_authorization_sri),
    CONSTRAINT guide_carrier_fk 
        FOREIGN KEY (id_carrier) REFERENCES carrier(id_carrier)
);

-- 4. Crear tabla guide_detail
CREATE TABLE guide_detail (
    id_guide_detail BIGINT NOT NULL PRIMARY KEY,
    id_guide BIGINT NOT NULL,
    id_product BIGINT,
    description VARCHAR(300) NOT NULL,
    quantity NUMERIC(19, 2) NOT NULL,
    unit VARCHAR(10),
    weight NUMERIC(19, 2),
    additional_info TEXT,
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guide_detail_guide 
        FOREIGN KEY (id_guide) REFERENCES guide(id_guide) ON DELETE CASCADE
);

-- 5. Crear tabla guide_document_reference
CREATE TABLE guide_document_reference (
    id_guide_document_reference BIGINT NOT NULL PRIMARY KEY,
    id_guide BIGINT NOT NULL,
    document_type VARCHAR(2) NOT NULL,
    document_number VARCHAR(17) NOT NULL,
    access_key VARCHAR(49),
    issue_date TIMESTAMP,
    status BOOLEAN DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_guide_document_reference_guide 
        FOREIGN KEY (id_guide) REFERENCES guide(id_guide) ON DELETE CASCADE
);

-- 6. Crear índices
CREATE INDEX idx_guide_company ON guide(id_company);
CREATE INDEX idx_guide_client ON guide(id_client);
CREATE INDEX idx_guide_establishment ON guide(id_establishment);
CREATE INDEX idx_guide_issuance_point ON guide(id_issuance_point, sequential);
CREATE INDEX idx_guide_issue_date ON guide(issue_date);
CREATE INDEX idx_guide_number ON guide(guide_number);
CREATE INDEX idx_guide_authorization ON guide(id_authorization_sri);
CREATE INDEX idx_guide_carrier ON guide(id_carrier);
CREATE INDEX idx_guide_detail_guide ON guide_detail(id_guide);
CREATE INDEX idx_guide_document_reference_guide ON guide_document_reference(id_guide);
CREATE INDEX idx_guide_document_reference_type ON guide_document_reference(document_type, document_number);
CREATE INDEX idx_carrier_company ON carrier(id_company);
CREATE INDEX idx_carrier_identification ON carrier(identification);

-- 7. Activar trigger de suscripciones para guide
DROP TRIGGER IF EXISTS trigger_guide_usage ON guide;

CREATE TRIGGER trigger_guide_usage
AFTER INSERT ON guide
FOR EACH ROW
WHEN (NEW.id_authorization_sri IS NOT NULL)
EXECUTE FUNCTION increment_document_usage();

COMMENT ON TRIGGER trigger_guide_usage ON guide IS 
'Incrementa contadores de suscripción cuando se autoriza una guía de remisión';

-- 8. Comentarios
COMMENT ON TABLE guide IS 'Tabla de guías de remisión';
COMMENT ON TABLE guide_detail IS 'Tabla de detalles/productos a transportar en la guía de remisión';
COMMENT ON TABLE guide_document_reference IS 'Tabla de documentos relacionados con la guía (facturas, notas de crédito, etc.)';
COMMENT ON TABLE carrier IS 'Catálogo de transportistas';
COMMENT ON COLUMN guide.id_client IS 'ID del cliente destinatario (consultado desde ebill-customer vía Feign)';
COMMENT ON COLUMN guide.id_establishment IS 'ID del establecimiento emisor';
COMMENT ON COLUMN guide.id_issuance_point IS 'ID del punto de emisión';
COMMENT ON COLUMN guide.sequential IS 'Número secuencial por punto de emisión';
COMMENT ON COLUMN guide.guide_number IS 'Número de guía (formato: estab-ptoEmi-secuencial, 17 caracteres)';
COMMENT ON COLUMN guide.start_date IS 'Fecha inicio de traslado';
COMMENT ON COLUMN guide.end_date IS 'Fecha fin de traslado';
COMMENT ON COLUMN guide.transport_type IS 'Tipo de transporte (terrestre, aéreo, marítimo, etc.)';
COMMENT ON COLUMN guide.transport_plate IS 'Placa del vehículo';
COMMENT ON COLUMN guide.id_carrier IS 'FK a transportista en catálogo (OBLIGATORIO, debe existir en carrier)';
COMMENT ON COLUMN guide.origin_address IS 'Dirección de partida';
COMMENT ON COLUMN guide.destination_address IS 'Dirección de destino';




