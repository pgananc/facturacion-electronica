# Flujo de Uso del Parámetro: MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS

## 📍 Ubicación del Parámetro

**Base de Datos:**
- **Tabla:** `public.params`
- **Código:** `MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS`
- **Valor por defecto:** `"10"`
- **Migración:** `V2026010200__add_comprehensive_rate_limiting_params.sql`

---

## 🔄 Flujo de Uso

### 1. **Punto de Entrada: Controlador**

**Archivo:** `src/main/java/com/wallparisoft/authentication/controller/MailController.java`

```java
@PostMapping(value = "/send", consumes = MediaType.TEXT_PLAIN_VALUE)
public ResponseEntity<Integer> send(@RequestBody String mail, HttpServletRequest request) {
    String clientIp = RequestUtils.extractClientIp(request);
    result = userService.sendMailUser(mail, clientIp);  // ← Llama al servicio
    return new ResponseEntity<>(result, HttpStatus.OK);
}
```

---

### 2. **Servicio: Validación de Rate Limiting**

**Archivo:** `src/main/java/com/wallparisoft/authentication/service/impl/UserServiceImpl.java`

**Línea 309:** Se obtiene el valor del parámetro

```java
@Override
@Transactional
public Integer sendMailUser(String mail, String clientIp) {
    // Validar rate limiting por email
    if (!rateLimitService.canMakeRequest(
        "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.EMAIL, mail)) {
        
        // ... código para obtener información del error ...
        
        // ⭐ AQUÍ SE USA EL PARÁMETRO
        int maxAttempts = rateLimitService.getMaxRequests(
            "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.EMAIL);
        // ↑ Este método lee el parámetro MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS
        
        // ... resto del código ...
    }
}
```

---

### 3. **Servicio Genérico: Lectura del Parámetro**

**Archivo:** `src/main/java/com/wallparisoft/authentication/service/GenericRateLimitService.java`

#### Método: `getMaxRequests()`

**Líneas 108-124:**

```java
@Override
public int getMaxRequests(String endpoint, RateLimitDimension dimension) {
    try {
        // ⭐ Construye el código del parámetro dinámicamente
        String codeParam = buildMaxRequestsCodeParam(endpoint, dimension);
        // Para "MAIL_SEND" + "EMAIL" → "MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS"
        
        // ⭐ Lee el parámetro desde la base de datos
        Params param = paramsRepository.findParamsByCodeParam(codeParam);
        
        if (param != null) {
            // ⭐ Convierte el valor string a entero
            return Integer.parseInt(param.getValueParam());
            // Si el parámetro tiene valor "10", retorna 10
        }
    } catch (Exception e) {
        log.warn("Error al obtener max requests, usando valor por defecto: 5", e);
    }
    // Valor por defecto si falla la lectura
    return 5;
}
```

#### Método Auxiliar: `buildMaxRequestsCodeParam()`

**Líneas 186-193:**

```java
private String buildMaxRequestsCodeParam(String endpoint, RateLimitDimension dimension) {
    String dimensionSuffix = getDimensionSuffix(dimension);
    // Para EMAIL → "EMAIL"
    
    // Para validación de token usamos MAX_ATTEMPTS, para otros MAX_REQUESTS
    if (endpoint.equals("TOKEN_VALIDATE")) {
        return String.format("%s_RATE_LIMIT_%s_MAX_ATTEMPTS", endpoint, dimensionSuffix);
    }
    
    // ⭐ Para MAIL_SEND construye: "MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS"
    return String.format("%s_RATE_LIMIT_%s_MAX_REQUESTS", endpoint, dimensionSuffix);
}
```

---

## 📊 Resumen del Flujo

```
1. Cliente hace POST /api/mail/send
   ↓
2. MailController.send() recibe la solicitud
   ↓
3. UserServiceImpl.sendMailUser() valida rate limiting
   ↓
4. rateLimitService.canMakeRequest("MAIL_SEND", EMAIL, mail)
   ↓
5. Internamente llama a getMaxRequests("MAIL_SEND", EMAIL)
   ↓
6. buildMaxRequestsCodeParam() construye: "MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS"
   ↓
7. paramsRepository.findParamsByCodeParam("MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS")
   ↓
8. Lee de BD: value_param = "10"
   ↓
9. Integer.parseInt("10") → retorna 10
   ↓
10. Compara: currentAttempts < 10
   ↓
11. Si excede, lanza RateLimitExceededException con maxAttempts = 10
```

---

## 🔍 Ubicaciones Específicas en el Código

### Línea 309 - UserServiceImpl.java
```java
int maxAttempts = rateLimitService.getMaxRequests(
    "MAIL_SEND", IGenericRateLimitService.RateLimitDimension.EMAIL);
```
**Propósito:** Obtener el límite máximo para mostrar en el mensaje de error

### Línea 108 - GenericRateLimitService.java
```java
public int getMaxRequests(String endpoint, RateLimitDimension dimension)
```
**Propósito:** Método público que lee el parámetro desde BD

### Línea 111 - GenericRateLimitService.java
```java
Params param = paramsRepository.findParamsByCodeParam(codeParam);
```
**Propósito:** Consulta directa a la base de datos

### Línea 113 - GenericRateLimitService.java
```java
return Integer.parseInt(param.getValueParam());
```
**Propósito:** Convierte el valor string del parámetro a entero

### Línea 192 - GenericRateLimitService.java
```java
return String.format("%s_RATE_LIMIT_%s_MAX_REQUESTS", endpoint, dimensionSuffix);
```
**Propósito:** Construye dinámicamente el código del parámetro

---

## 🎯 Uso del Valor

El valor del parámetro se utiliza para:

1. **Validación:** Comparar si `currentAttempts < maxAttempts`
2. **Mensaje de Error:** Mostrar al usuario cuántos intentos ha usado y cuál es el límite
3. **Lógica de Negocio:** Determinar si se permite o rechaza la solicitud

**Ejemplo de uso en mensaje de error:**
```java
String message = String.format(
    "Máximo de solicitudes alcanzado por email (%d/%d). Intente nuevamente en %d minutos",
    currentAttempts,  // Ej: 10
    maxAttempts,      // ← Valor del parámetro: 10
    minutesLeft       // Ej: 45
);
// Resultado: "Máximo de solicitudes alcanzado por email (10/10). Intente nuevamente en 45 minutos"
```

---

## 🔧 Cómo Modificar el Valor

### Desde Base de Datos (Sin redeploy):

```sql
-- Ver el valor actual
SELECT code_param, value_param 
FROM public.params 
WHERE code_param = 'MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS';

-- Cambiar el valor (ejemplo: de 10 a 15)
UPDATE public.params 
SET value_param = '15' 
WHERE code_param = 'MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS';
```

**Nota:** El cambio se aplica inmediatamente en la siguiente solicitud (no hay cache de parámetros).

---

## ⚠️ Valores por Defecto

Si el parámetro no existe o hay un error al leerlo:

- **Valor por defecto:** `5` (conservador)
- **Ubicación:** Línea 123 de `GenericRateLimitService.java`
- **Log:** Se registra un warning en los logs

```java
log.warn("Error al obtener max requests para {} - {}, usando valor por defecto: 5",
    endpoint, dimension, e);
return 5; // Valor por defecto conservador
```

---

## 📝 Relación con Otros Parámetros

El parámetro `MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS` trabaja junto con:

- `MAIL_SEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES` - Define la ventana de tiempo (60 minutos)
- `MAIL_SEND_RATE_LIMIT_IP_MAX_REQUESTS` - Límite por IP (10 solicitudes)
- `MAIL_SEND_RATE_LIMIT_IP_WINDOW_MINUTES` - Ventana de tiempo por IP (60 minutos)

Todos se leen de la misma manera, usando el mismo flujo pero con diferentes códigos de parámetro.

