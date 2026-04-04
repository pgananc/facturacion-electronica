INSERT INTO payment_method(
    id_payment_method, payment_method_type, status, creation_date, update_date, payment_method_code)
VALUES
    (1, 'SIN UTILIZACIÓN DEL SISTEMA FINANCIERO', true, NOW(), NOW(), '01'),
    (2, 'COMPENSACIÓN DE DEUDAS', true, NOW(), NOW(), '15'),
    (3, 'TARJETA DE DÉBITO', true, NOW(), NOW(), '16'),
    (4, 'DINERO ELECTRÓNICO', true, NOW(), NOW(), '17'),
    (5, 'TARJETA PREPAGO', true, NOW(), NOW(), '18'),
    (6, 'TARJETA DE CRÉDITO', true, NOW(), NOW(), '19'),
    (7, 'OTROS CON UTILIZACIÓN DEL SISTEMA FINANCIERO', true, NOW(), NOW(), '20'),
    (8, 'ENDOSO DE TÍTULOS', true, NOW(), NOW(), '21');