-- =====================================================
-- Tabla: session_fingerprint
-- Descripción: Almacena información de fingerprint de sesiones
--              para validar que el usuario accede desde el mismo
--              dispositivo/navegador donde inició sesión.
-- =====================================================

CREATE TABLE session_fingerprint (
    id BIGSERIAL PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    ip_address VARCHAR(45) NOT NULL,        -- IPv4 o IPv6
    user_agent VARCHAR(500) NOT NULL,       -- User-Agent completo
    browser_base VARCHAR(50) NOT NULL,      -- "Chrome", "Firefox", "Safari", etc.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_session_fingerprint_user FOREIGN KEY (user_id) REFERENCES users(id_user)
);

CREATE INDEX idx_session_fingerprint_user ON session_fingerprint(user_id);
CREATE INDEX idx_session_fingerprint_session_id ON session_fingerprint(session_id);

COMMENT ON TABLE session_fingerprint IS 'Almacena fingerprints de sesión para validación de dispositivo/navegador';
COMMENT ON COLUMN session_fingerprint.session_id IS 'UUID único de la sesión';
COMMENT ON COLUMN session_fingerprint.ip_address IS 'Dirección IP desde donde se inició sesión (IPv4 o IPv6)';
COMMENT ON COLUMN session_fingerprint.user_agent IS 'User-Agent completo del navegador';
COMMENT ON COLUMN session_fingerprint.browser_base IS 'Navegador base extraído del User-Agent (Chrome, Firefox, Safari, etc.)';

