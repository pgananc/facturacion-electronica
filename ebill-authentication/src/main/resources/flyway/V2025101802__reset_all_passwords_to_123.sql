-- Actualizar todos los usuarios con contraseña "123"
-- password: Hash bcrypt de "123" para autenticación
-- password_encrypted: "123" encriptado con AES-256 para recuperación por superadmins
-- Los usuarios deberán cambiar su contraseña al primer login

UPDATE public.users
SET 
    password = '$2a$10$f3vDxE4TVzki383BDDHFLuJYYTNI.paDch6o.mbFhTSvFeIAWiOh6',
    password_encrypted = 'zf5zxitAWzcnUrNj2DW4Ag=='
WHERE status = true;

-- Valores generados y verificados:
-- Hash bcrypt de "123": $2a$10$f3vDxE4TVzki383BDDHFLuJYYTNI.paDch6o.mbFhTSvFeIAWiOh6
-- AES-256 de "123": zf5zxitAWzcnUrNj2DW4Ag==
-- Secret key: W@llp@r1S0ft2024Secret!
-- Salt: W@llp@r1S@lt2024

