ALTER TABLE authorization_sri
ADD COLUMN IF NOT EXISTS authorization_storage_path TEXT;
COMMENT ON COLUMN authorization_sri.authorization_storage_path IS 'Directorio donde se almacenan los archivos autorizados (XML, PDF) en el sistema de archivos';

