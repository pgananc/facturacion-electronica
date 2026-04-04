-- Agregar campo para almacenar contraseña encriptada (reversible)
-- Este campo permite a superadmins recuperar contraseñas
-- mientras password_hash (bcrypt) mantiene la seguridad para login

ALTER TABLE public.users 
ADD COLUMN password_encrypted VARCHAR(500);

COMMENT ON COLUMN public.users.password_encrypted IS 'Contraseña encriptada con AES-256 (reversible para superadmins)';

