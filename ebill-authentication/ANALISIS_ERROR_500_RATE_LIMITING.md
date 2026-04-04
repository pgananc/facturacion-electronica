# Análisis: Error 500 en lugar de Mensaje de Rate Limiting

## 🔴 Problema Identificado

Cuando se excede el rate limiting, el servicio devuelve un **500 Internal Server Error** en lugar del mensaje apropiado de rate limiting.

---

## 🐛 Problema #1: MailController captura y oculta la excepción

**Archivo:** `src/main/java/com/wallparisoft/authentication/controller/MailController.java`

**Líneas 48-53:**

```java
try {
  String clientIp = RequestUtils.extractClientIp(request);
  result = userService.sendMailUser(mail, clientIp);
} catch (Exception e) {  // ⚠️ PROBLEMA: Captura TODAS las excepciones
  throw new InternalErrorException("Error al enviar correo");  // ⚠️ Oculta RateLimitExceededException
}
```

### ¿Qué está pasando?

1. `UserServiceImpl.sendMailUser()` lanza `RateLimitExceededException` cuando se excede el límite
2. El bloque `catch (Exception e)` en `MailController` captura esta excepción
3. La convierte en `InternalErrorException` con mensaje genérico "Error al enviar correo"
4. Se pierde toda la información del rate limiting (intentos, tiempo restante, etc.)

### Flujo actual (incorrecto):

```
UserServiceImpl.sendMailUser()
  ↓ lanza RateLimitExceededException("Máximo de solicitudes alcanzado...")
  ↓
MailController.send()
  ↓ catch (Exception e) ← Captura RateLimitExceededException
  ↓ throw new InternalErrorException("Error al enviar correo") ← Oculta la excepción original
  ↓
ResponseExceptionHandler.handleAllExceptions()
  ↓ Devuelve 500 Internal Server Error
```

---

## 🐛 Problema #2: No hay manejador específico para RateLimitExceededException

**Archivo:** `src/main/java/com/wallparisoft/authentication/exception/ResponseExceptionHandler.java`

**Líneas 36-41:**

```java
// Manejo de excepciones genéricas
@ExceptionHandler(Exception.class)  // ⚠️ Captura TODAS las excepciones no manejadas
public ResponseEntity<ExceptionResponse> handleAllExceptions(Exception ex, WebRequest request) {
  log.error("Excepción inesperada", ex);
  return buildResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR);  // ⚠️ Siempre devuelve 500
}
```

### ¿Qué está pasando?

1. No existe un `@ExceptionHandler(RateLimitExceededException.class)` específico
2. Si la excepción llegara al handler global, sería capturada por `@ExceptionHandler(Exception.class)`
3. Esto devolvería un **500 Internal Server Error** en lugar de un **429 Too Many Requests** (código HTTP apropiado para rate limiting)

### Excepciones que SÍ tienen manejador:

- ✅ `ModelNotFoundException` → 404 Not Found
- ✅ `ResourceExistsException` → 409 Conflict
- ✅ `ResourceInUseException` → 409 Conflict
- ✅ `InternalException` → 500 Internal Server Error
- ❌ `RateLimitExceededException` → **NO TIENE MANEJADOR** → Cae en Exception.class → 500

---

## 📊 Comparación: Cómo debería funcionar vs Cómo funciona

### ✅ Cómo DEBERÍA funcionar:

```
UserServiceImpl.sendMailUser()
  ↓ lanza RateLimitExceededException
  ↓
ResponseExceptionHandler.handleRateLimitExceeded()
  ↓ @ExceptionHandler(RateLimitExceededException.class)
  ↓ Devuelve 429 Too Many Requests
  ↓ Con mensaje: "Máximo de solicitudes alcanzado por email (10/10)..."
```

### ❌ Cómo funciona ACTUALMENTE:

```
UserServiceImpl.sendMailUser()
  ↓ lanza RateLimitExceededException
  ↓
MailController.send()
  ↓ catch (Exception e) ← Captura y oculta
  ↓ throw new InternalErrorException("Error al enviar correo")
  ↓
ResponseExceptionHandler.handleAllExceptions()
  ↓ @ExceptionHandler(Exception.class)
  ↓ Devuelve 500 Internal Server Error
  ↓ Con mensaje: "Error al enviar correo" (genérico, sin información útil)
```

---

## 🔍 Evidencia del Problema

### En el Debugger (imagen 1):
- ✅ `RateLimitExceededException` se lanza correctamente
- ✅ Tiene todos los datos: `currentAttempts=1`, `maxAttempts=1`, `minutesLeft=53`
- ✅ El mensaje es correcto: "Máximo de solicitudes alcanzado por email (1/1)..."

### En el Navegador (imagen 2):
- ❌ Status: `500 Internal Server Error`
- ❌ No se muestra el mensaje de rate limiting
- ❌ Se muestra un error genérico

---

## 🛠️ Soluciones Necesarias

### Solución 1: Modificar MailController

**Cambio necesario en `MailController.java`:**

```java
// ❌ ACTUAL (incorrecto):
try {
  String clientIp = RequestUtils.extractClientIp(request);
  result = userService.sendMailUser(mail, clientIp);
} catch (Exception e) {
  throw new InternalErrorException("Error al enviar correo");
}

// ✅ DEBERÍA SER:
try {
  String clientIp = RequestUtils.extractClientIp(request);
  result = userService.sendMailUser(mail, clientIp);
} catch (RateLimitExceededException e) {
  // Re-lanzar para que el handler global lo maneje
  throw e;
} catch (Exception e) {
  // Solo capturar otras excepciones
  throw new InternalErrorException("Error al enviar correo");
}
```

**O mejor aún, eliminar el try-catch y dejar que el handler global maneje todo:**

```java
// ✅ MEJOR OPCIÓN:
@PostMapping(value = "/send", consumes = MediaType.TEXT_PLAIN_VALUE)
public ResponseEntity<Integer> send(@RequestBody String mail, HttpServletRequest request) {
  // ... logs ...
  String clientIp = RequestUtils.extractClientIp(request);
  int result = userService.sendMailUser(mail, clientIp);
  // ... logs ...
  return new ResponseEntity<>(result, HttpStatus.OK);
}
```

---

### Solución 2: Agregar manejador específico en ResponseExceptionHandler

**Cambio necesario en `ResponseExceptionHandler.java`:**

```java
// Agregar después de la línea 65:

// Rate limiting excedido
@ExceptionHandler(RateLimitExceededException.class)
public ResponseEntity<RateLimitExceededResponse> handleRateLimitExceeded(
    RateLimitExceededException ex, WebRequest request) {
  log.warn("Rate limit excedido: {}", ex.getMessage());
  
  RateLimitExceededResponse response = RateLimitExceededResponse.builder()
      .timestamp(ex.getTimestamp())
      .message(ex.getMessage())
      .attemptsUsed(ex.getAttemptsUsed())
      .maxAttempts(ex.getMaxAttempts())
      .minutesUntilNextAttempt(ex.getMinutesUntilNextAttempt())
      .cooldownMinutes(ex.getCooldownMinutes())
      .build();
  
  return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS); // 429
}
```

**Y crear la clase de respuesta:**

```java
@Data
@Builder
public class RateLimitExceededResponse {
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime timestamp;
  private String message;
  private int attemptsUsed;
  private int maxAttempts;
  private long minutesUntilNextAttempt;
  private int cooldownMinutes;
}
```

---

## 📝 Resumen de Problemas

| Problema | Ubicación | Impacto |
|----------|-----------|---------|
| **1. Try-catch genérico oculta excepción** | `MailController.java:48-53` | Convierte `RateLimitExceededException` en `InternalErrorException` |
| **2. No hay manejador específico** | `ResponseExceptionHandler.java` | `RateLimitExceededException` cae en `Exception.class` → 500 |
| **3. Código HTTP incorrecto** | `ResponseExceptionHandler.java` | Debería ser 429, no 500 |
| **4. Pérdida de información** | `MailController.java:52` | Se pierden todos los detalles del rate limiting |

---

## ✅ Resultado Esperado Después de Corregir

**Request:**
```http
POST /api/mail/send
Content-Type: text/plain

pablos.ganan@gmail.com
```

**Response (429 Too Many Requests):**
```json
{
  "timestamp": "2026-01-06 15:39:57",
  "message": "Máximo de solicitudes alcanzado por email (10/10). Intente nuevamente en 53 minutos",
  "attemptsUsed": 10,
  "maxAttempts": 10,
  "minutesUntilNextAttempt": 53,
  "cooldownMinutes": 60
}
```

---

## 🔗 Archivos Afectados

1. **`MailController.java`** - Líneas 48-53: Eliminar o modificar el try-catch
2. **`ResponseExceptionHandler.java`** - Agregar manejador para `RateLimitExceededException`
3. **Crear `RateLimitExceededResponse.java`** - Nueva clase para la respuesta estructurada

---

## ⚠️ Nota Importante

El mismo problema puede existir en otros controladores que usen rate limiting:
- `TokenController.java` - Verificar si tiene try-catch similar

