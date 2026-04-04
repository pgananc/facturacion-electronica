-- Agregar parámetro para mapeo de regímenes tributarios
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
        'COMPANY_REGIME_MAPPING',
        'GENERAL|General,RIMPE_POPULAR|RIMPE Negocio Popular,RIMPE_STARTUP|RIMPE Emprendedor,SPECIAL|Especial',
        'Mapeo de códigos de régimen tributario a etiquetas para mostrar en el RIDE. Formato: clave|valor,clave|valor',
        TRUE,
        CURRENT_TIMESTAMP
    ) ON CONFLICT (code_param) DO
UPDATE
SET value_param = 'GENERAL|General,RIMPE_POPULAR|RIMPE Negocio Popular,RIMPE_STARTUP|RIMPE Emprendedor,SPECIAL|Especial',
    description_param = 'Mapeo de códigos de régimen tributario a etiquetas para mostrar en el RIDE. Formato: clave|valor,clave|valor',
    update_date = CURRENT_TIMESTAMP;