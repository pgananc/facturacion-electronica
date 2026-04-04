-- Agregar parámetro para la ruta de almacenamiento de notas de crédito
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
        'CREDIT_NOTE_STORAGE_BASE_DIR',
        '/opt/ebilling/credit_notes',
        'Ruta base donde se almacenan los archivos PDF y XML de las notas de crédito',
        TRUE,
        CURRENT_TIMESTAMP
    ) ON CONFLICT (code_param) DO NOTHING;

