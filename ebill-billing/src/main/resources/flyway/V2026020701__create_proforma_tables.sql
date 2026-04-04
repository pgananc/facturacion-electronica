-- =====================================================
-- PROFORMA (QUOTES / PRE-INVOICES)
-- Version: 1.0
-- Date: 2026-02-07
-- =====================================================
-- Table proforma: header, scoped by company, establishment, issuance point.
-- Company/establishment/issuance_point/client are IDs referencing ebill-customer (no FK in this DB).
-- =====================================================
CREATE TABLE IF NOT EXISTS proforma (
    id_proforma BIGSERIAL PRIMARY KEY,
    id_company BIGINT NOT NULL,
    id_establishment BIGINT NOT NULL,
    id_issuance_point BIGINT NOT NULL,
    id_client BIGINT,
    proforma_number VARCHAR(50) NOT NULL,
    issue_date DATE NOT NULL,
    valid_until DATE,
    status VARCHAR(20) NOT NULL,
    id_invoice BIGINT,
    additional_information VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    CONSTRAINT uq_proforma_company_number UNIQUE (id_company, proforma_number),
    CONSTRAINT chk_proforma_status CHECK (status IN ('DRAFT', 'SENT', 'CONVERTED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_proforma_id_company ON proforma (id_company);
CREATE INDEX IF NOT EXISTS idx_proforma_issue_date ON proforma (issue_date);
CREATE INDEX IF NOT EXISTS idx_proforma_status ON proforma (status);
CREATE INDEX IF NOT EXISTS idx_proforma_id_client ON proforma (id_client);

-- Optional FK to invoice when converted (invoice table exists in this DB)
ALTER TABLE proforma
    ADD CONSTRAINT fk_proforma_invoice
    FOREIGN KEY (id_invoice) REFERENCES invoice(id_invoice) ON DELETE SET NULL;

-- =====================================================
-- Table proforma_detail: line items
-- =====================================================
CREATE TABLE IF NOT EXISTS proforma_detail (
    id_proforma_detail BIGSERIAL PRIMARY KEY,
    id_proforma BIGINT NOT NULL,
    id_product BIGINT NOT NULL,
    product_code VARCHAR(50),
    description VARCHAR(500),
    quantity DECIMAL(19,4) NOT NULL,
    unit_price DECIMAL(19,4) NOT NULL,
    discount DECIMAL(19,4) NOT NULL DEFAULT 0,
    iva_percentage DECIMAL(5,2) NOT NULL DEFAULT 0,
    ice_percentage DECIMAL(5,2) NOT NULL DEFAULT 0,
    subtotal DECIMAL(19,4) NOT NULL,
    line_order INT NOT NULL,
    CONSTRAINT fk_proforma_detail_proforma FOREIGN KEY (id_proforma) REFERENCES proforma(id_proforma) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_proforma_detail_id_proforma ON proforma_detail (id_proforma);

-- Sequences
CREATE SEQUENCE IF NOT EXISTS proforma_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS proforma_detail_seq START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE proforma IS 'Proformas (quotes / pre-invoices), not sent to SRI. Can be converted to invoice.';
COMMENT ON TABLE proforma_detail IS 'Line items of a proforma.';
COMMENT ON COLUMN proforma.status IS 'DRAFT, SENT, CONVERTED, EXPIRED';
COMMENT ON COLUMN proforma.id_invoice IS 'Set when proforma is converted to invoice (draft or authorized).';
