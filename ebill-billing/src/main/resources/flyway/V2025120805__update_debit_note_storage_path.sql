-- Actualizar la ruta de almacenamiento de notas de débito a /opt/ebilling/credit_debit
-- Si el parámetro no existe, se crea; si existe, se actualiza

INSERT INTO params (
    id_param,
    code_param,
    value_param,
    description_param,
    status,
    creation_date,
    update_date
)
VALUES (
    (
        SELECT COALESCE(MAX(id_param), 0) + 1
        FROM params
    ),
    'DEBIT_NOTE_STORAGE_BASE_DIR',
    '/opt/ebilling/credit_debit',
    'Ruta base donde se almacenan los archivos PDF y XML de las notas de débito',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code_param) 
DO UPDATE SET 
    value_param = '/opt/ebilling/credit_debit',
    description_param = 'Ruta base donde se almacenan los archivos PDF y XML de las notas de débito',
    update_date = CURRENT_TIMESTAMP;

