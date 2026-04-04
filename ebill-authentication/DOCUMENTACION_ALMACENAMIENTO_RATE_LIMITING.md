# Documentación: Almacenamiento del Rate Limiting

## 📍 Ubicación del Almacenamiento

El rate limiting utiliza **dos ubicaciones** para almacenar información:

### 1. **Redis** - Contadores de Rate Limiting (Temporal)

Los **contadores de solicitudes/intentos** se almacenan en **Redis** usando la clase `UtilsRedis`.

#### Formato de las Claves en Redis

Las claves siguen este patrón:
```
rate_limit:{ENDPOINT}:{DIMENSION}:{IDENTIFIER}
```

**Ejemplos de claves:**
- `rate_limit:MAIL_SEND:EMAIL:usuario@example.com`
- `rate_limit:MAIL_SEND:IP:192.168.1.100`
- `rate_limit:TOKEN_CLIENT:EMAIL:cliente@example.com`
- `rate_limit:TOKEN_CLIENT:IP:10.0.0.50`
- `rate_limit:TOKEN_VALIDATE:IP:192.168.1.100`
- `rate_limit:TOKEN_VALIDATE:CLIENT:1234567890`
- `rate_limit:TOKEN_RESEND:EMAIL:cliente@example.com`
- `rate_limit:TOKEN_RESEND:IP:10.0.0.50`

#### Valor Almacenado

- **Tipo:** String (número como texto)
- **Contenido:** Número de solicitudes/intentos realizados
- **Ejemplo:** `"5"` (indica 5 solicitudes realizadas)

#### TTL (Time To Live)

- Cada clave tiene un **TTL** (tiempo de expiración) configurado en **segundos**
- El TTL se calcula desde los parámetros de configuración: `{ENDPOINT}_RATE_LIMIT_{DIMENSION}_WINDOW_MINUTES`
- Cuando expira el TTL, la clave se elimina automáticamente de Redis
- Esto permite que el contador se resetee automáticamente después del período de tiempo

**Ejemplo:**
- Si `MAIL_SEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES = 60`
- El TTL será: `60 minutos * 60 = 3600 segundos`
- Después de 1 hora, la clave expira y el contador se resetea

#### Ubicación del Código

**Archivo:** `src/main/java/com/wallparisoft/authentication/service/GenericRateLimitService.java`

**Métodos clave:**
```java
// Construir clave de Redis
private String buildRateLimitKey(String endpoint, RateLimitDimension dimension, String identifier) {
    return String.format("rate_limit:%s:%s:%s", endpoint, dimension.name(), identifier);
}

// Incrementar contador (guarda en Redis)
public void incrementRequest(...) {
    Long attempts = utilsRedis.incrementKey(key);
    if (attempts == 1) {
        long ttlSeconds = windowMinutes * 60;
        utilsRedis.setKeyWithTTL(key, "1", ttlSeconds);
    }
}

// Leer contador (lee de Redis)
public boolean canMakeRequest(...) {
    String attempts = utilsRedis.getKey(key);
    // ...
}
```

---

### 2. **Base de Datos PostgreSQL** - Configuración (Permanente)

Los **parámetros de configuración** (límites máximos y ventanas de tiempo) se almacenan en la **tabla `params`** de PostgreSQL.

#### Tabla: `public.params`

**Estructura:**
- `id_param` - ID único
- `code_param` - Código del parámetro (único)
- `value_param` - Valor del parámetro (texto)
- `description_param` - Descripción
- `status` - Estado (activo/inactivo)
- `creation_date` - Fecha de creación

#### Parámetros Almacenados

**Para `/api/mail/send`:**
- `MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS` = `"10"`
- `MAIL_SEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES` = `"60"`
- `MAIL_SEND_RATE_LIMIT_IP_MAX_REQUESTS` = `"10"`
- `MAIL_SEND_RATE_LIMIT_IP_WINDOW_MINUTES` = `"60"`

**Para `/api/token/client`:**
- `TOKEN_CLIENT_RATE_LIMIT_EMAIL_MAX_REQUESTS` = `"5"`
- `TOKEN_CLIENT_RATE_LIMIT_EMAIL_WINDOW_MINUTES` = `"60"`
- `TOKEN_CLIENT_RATE_LIMIT_IP_MAX_REQUESTS` = `"20"`
- `TOKEN_CLIENT_RATE_LIMIT_IP_WINDOW_MINUTES` = `"60"`

**Para `/api/token/validate-client/generate`:**
- `TOKEN_VALIDATE_RATE_LIMIT_IP_MAX_ATTEMPTS` = `"10"`
- `TOKEN_VALIDATE_RATE_LIMIT_IP_WINDOW_MINUTES` = `"15"`
- `TOKEN_VALIDATE_RATE_LIMIT_CLIENT_MAX_ATTEMPTS` = `"20"`
- `TOKEN_VALIDATE_RATE_LIMIT_CLIENT_WINDOW_MINUTES` = `"60"`

**Para `/api/token/resend-client`:**
- `TOKEN_RESEND_RATE_LIMIT_EMAIL_MAX_REQUESTS` = `"3"`
- `TOKEN_RESEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES` = `"60"`
- `TOKEN_RESEND_RATE_LIMIT_IP_MAX_REQUESTS` = `"10"`
- `TOKEN_RESEND_RATE_LIMIT_IP_WINDOW_MINUTES` = `"60"`

#### Migración Flyway

**Archivo:** `src/main/resources/flyway/V2026010200__add_comprehensive_rate_limiting_params.sql`

Esta migración crea todos los parámetros en la base de datos.

#### Ubicación del Código

**Archivo:** `src/main/java/com/wallparisoft/authentication/service/GenericRateLimitService.java`

**Métodos que leen de BD:**
```java
// Leer máximo de solicitudes desde BD
public int getMaxRequests(String endpoint, RateLimitDimension dimension) {
    String codeParam = buildMaxRequestsCodeParam(endpoint, dimension);
    Params param = paramsRepository.findParamsByCodeParam(codeParam);
    return Integer.parseInt(param.getValueParam());
}

// Leer ventana de tiempo desde BD
public int getWindowMinutes(String endpoint, RateLimitDimension dimension) {
    String codeParam = buildWindowMinutesCodeParam(endpoint, dimension);
    Params param = paramsRepository.findParamsByCodeParam(codeParam);
    return Integer.parseInt(param.getValueParam());
}
```

---

## 🔄 Flujo de Almacenamiento

### Cuando se realiza una solicitud:

1. **Validación (Lectura):**
   - Lee contador de Redis: `utilsRedis.getKey("rate_limit:MAIL_SEND:EMAIL:user@example.com")`
   - Lee configuración de BD: `paramsRepository.findParamsByCodeParam("MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS")`
   - Compara: `contador < maxRequests`

2. **Si se permite la solicitud:**
   - Incrementa contador en Redis: `utilsRedis.incrementKey(key)`
   - Si es el primer intento, establece TTL: `utilsRedis.setKeyWithTTL(key, "1", ttlSeconds)`

3. **Si se excede el límite:**
   - Lanza excepción: `RateLimitExceededException`
   - No incrementa el contador

### Ejemplo Práctico:

**Solicitud 1:** `/api/mail/send` con email `user@example.com`
- Redis: `rate_limit:MAIL_SEND:EMAIL:user@example.com` = `"1"` (TTL: 3600 segundos)
- BD: Lee `MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS` = `"10"`

**Solicitud 2-10:** Mismo email
- Redis: Incrementa contador hasta `"10"`
- BD: Sigue leyendo el mismo valor `"10"`

**Solicitud 11:** Mismo email (dentro de 1 hora)
- Redis: Contador = `"10"`
- Validación: `10 < 10` = `false` ❌
- Resultado: `RateLimitExceededException` - "Máximo de solicitudes alcanzado"

**Después de 1 hora:**
- Redis: La clave expira automáticamente (TTL)
- Solicitud nueva: Contador vuelve a `"1"` ✅

---

## 🔍 Cómo Inspeccionar el Almacenamiento

### Redis

**Usando Redis CLI:**
```bash
# Conectar a Redis
redis-cli

# Ver todas las claves de rate limiting
KEYS rate_limit:*

# Ver valor de una clave específica
GET rate_limit:MAIL_SEND:EMAIL:user@example.com

# Ver TTL restante (en segundos)
TTL rate_limit:MAIL_SEND:EMAIL:user@example.com

# Ver todas las claves con sus valores
MGET rate_limit:MAIL_SEND:EMAIL:user@example.com rate_limit:MAIL_SEND:IP:192.168.1.100
```

**Usando herramientas gráficas:**
- RedisInsight
- Redis Desktop Manager
- Redis Commander

### Base de Datos

**Consulta SQL:**
```sql
-- Ver todos los parámetros de rate limiting
SELECT code_param, value_param, description_param 
FROM public.params 
WHERE code_param LIKE '%RATE_LIMIT%' 
ORDER BY code_param;

-- Ver un parámetro específico
SELECT * FROM public.params 
WHERE code_param = 'MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS';

-- Actualizar un límite (ejemplo: cambiar a 15 solicitudes)
UPDATE public.params 
SET value_param = '15' 
WHERE code_param = 'MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS';
```

---

## ⚙️ Configuración de Redis

**Archivo de configuración:** `src/main/resources/application.properties` o `application-localhost.properties`

**Propiedades típicas:**
```properties
# Configuración de Redis (si está en application.properties)
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
spring.redis.database=0
```

**Nota:** La configuración exacta depende de cómo esté configurado `UtilsRedis` en la librería externa.

---

## 📊 Resumen

| Tipo de Dato | Ubicación | Persistencia | Propósito |
|--------------|-----------|--------------|-----------|
| **Contadores** | Redis | Temporal (TTL) | Contar solicitudes/intentos |
| **Configuración** | PostgreSQL (tabla `params`) | Permanente | Límites máximos y ventanas de tiempo |
| **Claves Redis** | Formato: `rate_limit:{endpoint}:{dimension}:{identifier}` | - | Identificadores únicos |
| **Valores Redis** | Números como String (ej: `"5"`) | - | Contador actual |

---

## 🔧 Mantenimiento

### Limpiar Contadores Manualmente

Si necesitas limpiar contadores de rate limiting (por ejemplo, para testing):

**Desde código:**
```java
genericRateLimitService.clearAttempts("MAIL_SEND", RateLimitDimension.EMAIL, "user@example.com");
```

**Desde Redis CLI:**
```bash
# Eliminar una clave específica
DEL rate_limit:MAIL_SEND:EMAIL:user@example.com

# Eliminar todas las claves de rate limiting
KEYS rate_limit:* | xargs redis-cli DEL
```

### Ajustar Configuración

Los límites se pueden ajustar **sin redeploy** modificando la tabla `params`:

```sql
-- Ejemplo: Aumentar límite de emails de 10 a 20
UPDATE public.params 
SET value_param = '20' 
WHERE code_param = 'MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS';
```

Los cambios se aplican en la siguiente solicitud (no hay cache de parámetros actualmente).

---

## 🚨 Consideraciones Importantes

1. **Redis debe estar disponible:** Si Redis falla, el rate limiting no funcionará correctamente
2. **TTL automático:** Las claves expiran automáticamente, no necesitan limpieza manual
3. **Configuración dinámica:** Los parámetros se pueden cambiar en BD sin redeploy
4. **Valores por defecto:** Si falla la lectura de parámetros, se usan valores conservadores (5 solicitudes, 60 minutos)
5. **Múltiples dimensiones:** Cada dimensión (EMAIL, IP, CLIENT) tiene su propio contador independiente

