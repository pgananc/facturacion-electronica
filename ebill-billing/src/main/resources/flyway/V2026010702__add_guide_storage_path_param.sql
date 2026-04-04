-- Agregar parámetro para la ruta de almacenamiento de guías de remisión
INSERT INTO params (
    id_param,
    code_param,
    value_param,
    description_param,
    status,
    creation_date
)
VALUES (
    (
        SELECT COALESCE(MAX(id_param), 0) + 1
        FROM params
    ),
    'GUIDE_STORAGE_BASE_DIR',
    '/opt/ebilling/guides',
    'Ruta base donde se almacenan los archivos PDF y XML de las guías de remisión',
    TRUE,
    CURRENT_TIMESTAMP
) ON CONFLICT (code_param) DO NOTHING;




