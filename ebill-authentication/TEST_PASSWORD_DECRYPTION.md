# 🧪 Test Plan - Verificación de Desencriptación de Passwords

## Paso 1: Limpiar y Compilar
```bash
cd /Users/jguerreroh/Development/wallparisoft/facturacion-electronica/ebill-authentication
mvn clean package -DskipTests
```

## Paso 2: Ejecutar la aplicación
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=localhost
```

O desde Cursor/IntelliJ: Ejecutar la configuración "🚀 Run ebill-authentication (localhost)"

## Paso 3: Verificar que Flyway aplicó las migraciones

1. Conectarse a la base de datos:
```bash
psql -h localhost -p 5433 -U postgres -d ebill-dev-auth
```

2. Verificar que el campo existe:
```sql
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'users' AND column_name = 'password_encrypted';
```

Debería mostrar:
```
column_name        | data_type
-------------------+-----------
password_encrypted | character varying
```

3. Verificar que los usuarios tienen passwords encriptados:
```sql
SELECT id_user, username, password_encrypted 
FROM users 
LIMIT 3;
```

Debería mostrar:
```
id_user | username   | password_encrypted
--------|------------|-------------------
1       | 1722633961 | zf5zxitAWzcnUrNj2DW4Ag==
2       | 1803861937 | zf5zxitAWzcnUrNj2DW4Ag==
```

## Paso 4: Probar el endpoint de listado

```bash
# 1. Hacer login (password es "123")
curl -X POST http://localhost:8280/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "1722633961",
    "password": "123"
  }'
```

Copiar el token del response.

```bash
# 2. Listar usuarios
curl -X POST 'http://localhost:8280/api/user/pageable?page=0&size=10' \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TU_TOKEN_AQUI" \
  -d '{
    "name": "",
    "userName": "",
    "status": true,
    "idCompany": 1
  }'
```

## ✅ Resultado Esperado:

El response debe incluir el campo `password` desencriptado:

```json
{
  "content": [
    {
      "idUser": 1,
      "userName": "1722633961",
      "password": "123",  // ✅ Desencriptado!
      "mail": "jguerreroh@outlook.com",
      "name": "Jonathan Guerrero",
      "status": true,
      "idCompany": 1,
      "roleDtos": [...]
    }
  ],
  ...
}
```

## ❌ Si el password viene null o vacío:

Posibles causas:

### 1. Flyway no ejecutó las migraciones
**Solución**: Verificar logs de inicio, buscar:
```
Flyway: Migrating schema ... to version 2025101801
Flyway: Migrating schema ... to version 2025101802
```

### 2. Campo password_encrypted está null
**Solución**: Ejecutar manualmente:
```sql
UPDATE public.users
SET password_encrypted = 'zf5zxitAWzcnUrNj2DW4Ag=='
WHERE status = true;
```

### 3. AESEncryptionUtil no se inyecta correctamente
**Solución**: Agregar logs al método decryptPassword:
```java
protected String decryptPassword(User user) {
    System.out.println("🔍 decryptPassword called for user: " + user.getUserName());
    System.out.println("🔍 passwordEncrypted: " + user.getPasswordEncrypted());
    System.out.println("🔍 aesEncryptionUtil is null? " + (aesEncryptionUtil == null));
    
    try {
        if (user.getPasswordEncrypted() != null && aesEncryptionUtil != null) {
            String decrypted = aesEncryptionUtil.decrypt(user.getPasswordEncrypted());
            System.out.println("🔍 Decrypted password: " + decrypted);
            return decrypted;
        }
        System.out.println("⚠️ passwordEncrypted or aesEncryptionUtil is null");
        return null;
    } catch (Exception e) {
        System.out.println("❌ Error decrypting: " + e.getMessage());
        e.printStackTrace();
        return null;
    }
}
```

Luego recompilar y volver a ejecutar.

---

## 📝 Reporta los resultados:

1. ¿Flyway aplicó las migraciones? (Sí/No)
2. ¿Los usuarios tienen password_encrypted en BD? (Sí/No)
3. ¿El endpoint retorna el campo password? (Sí/No/Null)
4. Si viene null, ¿qué muestran los logs de decryptPassword?

