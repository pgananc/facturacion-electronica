# Propuesta: Aplicar Rate Limiting al estilo del servicio de autenticación para `/documents/query`

## 📋 Resumen

Aplicar el mismo patrón de rate limiting usado en `ebill-authentication` (específicamente en `/api/mail/send`) al endpoint `/api/invoice/documents/query` del servicio `ebill-billing`.

## 🔍 Análisis del patrón actual en `ebill-authentication`

### Características clave:

1. **NO usa AOP**: El rate limiting se implementa directamente en el método del servicio
2. **Servicio genérico**: Usa `GenericRateLimitService` con métodos:
   - `canMakeRequest(endpoint, dimension, identifier)` → boolean
   - `incrementRequest(endpoint, dimension, identifier)` → void
   - `getTimeUntilNextAttempt(...)` → long (segundos)
   - `getCurrentAttempts(...)` → int
   - `getMaxRequests(...)` → int
   - `getWindowMinutes(...)` → int
3. **Verificación ANTES de procesar**: Se verifica el límite antes de ejecutar la lógica de negocio
4. **Incremento DESPUÉS de éxito**: Solo se incrementa el contador si la operación fue exitosa
5. **Excepción rica en información**: `RateLimitExceededException` incluye:
   - `minutesUntilNextAttempt`
   - `attemptsUsed`
   - `maxAttempts`
   - `cooldownMinutes`
6. **Respuesta estructurada**: El handler retorna `RateLimitExceededResponse` con toda la información

### Flujo en `MailController.send()`:

```java
@PostMapping(value = "/send", consumes = MediaType.TEXT_PLAIN_VALUE)
public ResponseEntity<Integer> send(@RequestBody String mail, HttpServletRequest request) {
    try {
        String clientIp = RequestUtils.extractClientIp(request);
        result = userService.sendMailUser(mail, clientIp); // Rate limiting dentro del servicio
    } catch (RateLimitExceededException e) {
        throw e; // Re-lanzar para que el handler global lo maneje
    }
    return new ResponseEntity<>(result, HttpStatus.OK);
}
```

### Flujo en `UserServiceImpl.sendMailUser()`:

```java
public Integer sendMailUser(String mail, String clientIp) {
    // 1. Validar rate limiting por EMAIL
    if (!rateLimitService.canMakeRequest("MAIL_SEND", EMAIL, mail)) {
        // Obtener información para el mensaje
        long secondsLeft = rateLimitService.getTimeUntilNextAttempt(...);
        int currentAttempts = rateLimitService.getCurrentAttempts(...);
        int maxAttempts = rateLimitService.getMaxRequests(...);
        int windowMinutes = rateLimitService.getWindowMinutes(...);
        
        // Lanzar excepción con información detallada
        throw new RateLimitExceededException(message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
    }
    
    // 2. Validar rate limiting por IP
    if (clientIp != null && !rateLimitService.canMakeRequest("MAIL_SEND", IP, clientIp)) {
        // Similar al anterior...
        throw new RateLimitExceededException(...);
    }
    
    // 3. Ejecutar lógica de negocio
    int result = 0;
    // ... lógica de envío de correo ...
    
    // 4. Incrementar contadores SOLO si fue exitoso
    rateLimitService.incrementRequest("MAIL_SEND", EMAIL, mail);
    if (clientIp != null) {
        rateLimitService.incrementRequest("MAIL_SEND", IP, clientIp);
    }
    
    return result;
}
```

## 🎯 Cambios propuestos para `ebill-billing`

### 1. Crear servicio genérico de rate limiting (similar a `GenericRateLimitService`)

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/GenericRateLimitService.java`

```java
package com.wallparisoft.ebillbilling.service;

public interface GenericRateLimitService {
    enum RateLimitDimension {
        TOKEN,
        IP
    }
    
    boolean canMakeRequest(String endpoint, RateLimitDimension dimension, String identifier);
    void incrementRequest(String endpoint, RateLimitDimension dimension, String identifier);
    long getTimeUntilNextAttempt(String endpoint, RateLimitDimension dimension, String identifier);
    int getCurrentAttempts(String endpoint, RateLimitDimension dimension, String identifier);
    int getMaxRequests(String endpoint, RateLimitDimension dimension);
    int getWindowMinutes(String endpoint, RateLimitDimension dimension);
}
```

**Implementación**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/GenericRateLimitServiceImpl.java`

- Usar Redis con TTL (similar al servicio de autenticación)
- Leer parámetros desde `params` table con formato:
  - `{ENDPOINT}_RATE_LIMIT_{DIMENSION}_MAX_REQUESTS`
  - `{ENDPOINT}_RATE_LIMIT_{DIMENSION}_WINDOW_MINUTES`
- Ejemplo para `/documents/query`:
  - `DOCUMENTS_QUERY_RATE_LIMIT_TOKEN_MAX_REQUESTS`
  - `DOCUMENTS_QUERY_RATE_LIMIT_TOKEN_WINDOW_MINUTES`
  - `DOCUMENTS_QUERY_RATE_LIMIT_IP_MAX_REQUESTS`
  - `DOCUMENTS_QUERY_RATE_LIMIT_IP_WINDOW_MINUTES`

### 2. Actualizar `RateLimitExceededException` para incluir más información

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/exception/RateLimitExceededException.java`

```java
public class RateLimitExceededException extends RuntimeException {
    private final LocalDateTime timestamp;
    private final long minutesUntilNextAttempt;
    private final int attemptsUsed;
    private final int maxAttempts;
    private final int cooldownMinutes;
    
    // Constructor con todos los parámetros
    // Getters para todos los campos
}
```

### 3. Crear DTO de respuesta estructurada

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/exception/RateLimitExceededResponse.java`

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

### 4. Actualizar `GlobalExceptionHandler` para retornar respuesta estructurada

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/exception/GlobalExceptionHandler.java`

```java
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
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add("Access-Control-Allow-Origin", "*");
    headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
    headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
    
    return new ResponseEntity<>(response, headers, HttpStatus.TOO_MANY_REQUESTS);
}
```

### 5. Remover anotación `@RateLimited` del controlador

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/controller/InvoiceController.java`

```java
@PostMapping("/documents/query")
// REMOVER: @RateLimited(type = RateLimitType.QUERY, endpoint = "/api/invoice/documents/query")
public ResponseEntity<Page<DocumentSearchResponseDto>> queryDocuments(
    @RequestBody DocumentQueryDto queryDto, 
    Pageable pageable,
    HttpServletRequest request) { // Agregar HttpServletRequest
    
    // ... logs ...
    
    Page<DocumentSearchResponseDto> documents = invoiceService.queryDocuments(queryDto, pageable, request);
    
    return new ResponseEntity<>(documents, HttpStatus.OK);
}
```

### 6. Implementar rate limiting en `InvoiceServiceImpl.queryDocuments()`

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java`

```java
public Page<DocumentSearchResponseDto> queryDocuments(
    DocumentQueryDto queryDto, 
    Pageable pageable,
    HttpServletRequest request) { // Agregar HttpServletRequest
    
    // Extraer IP del cliente
    String clientIp = extractClientIp(request);
    
    // Extraer token del request
    String token = extractToken(request);
    
    // 1. Validar rate limiting por TOKEN
    if (token != null && !token.isBlank()) {
        if (!genericRateLimitService.canMakeRequest(
            "DOCUMENTS_QUERY", GenericRateLimitService.RateLimitDimension.TOKEN, token)) {
            
            long secondsLeft = genericRateLimitService.getTimeUntilNextAttempt(
                "DOCUMENTS_QUERY", GenericRateLimitService.RateLimitDimension.TOKEN, token);
            long minutesLeft = secondsLeft / 60;
            int currentAttempts = genericRateLimitService.getCurrentAttempts(
                "DOCUMENTS_QUERY", GenericRateLimitService.RateLimitDimension.TOKEN, token);
            int maxAttempts = genericRateLimitService.getMaxRequests(
                "DOCUMENTS_QUERY", GenericRateLimitService.RateLimitDimension.TOKEN);
            int windowMinutes = genericRateLimitService.getWindowMinutes(
                "DOCUMENTS_QUERY", GenericRateLimitService.RateLimitDimension.TOKEN);
            
            String message = String.format(
                "Máximo de solicitudes alcanzado por token (%d/%d). Intente nuevamente en %d minutos",
                currentAttempts, maxAttempts, minutesLeft);
            
            throw new RateLimitExceededException(
                message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
        }
    }
    
    // 2. Validar rate limiting por IP
    if (clientIp != null && !clientIp.isBlank()) {
        if (!genericRateLimitService.canMakeRequest(
            "DOCUMENTS_QUERY", GenericRateLimitService.RateLimitDimension.IP, clientIp)) {
            
            long secondsLeft = genericRateLimitService.getTimeUntilNextAttempt(
                "DOCUMENTS_QUERY", GenericRateLimitService.RateLimitDimension.IP, clientIp);
            long minutesLeft = secondsLeft / 60;
            int currentAttempts = genericRateLimitService.getCurrentAttempts(
                "DOCUMENTS_QUERY", GenericRateLimitService.RateLimitDimension.IP, clientIp);
            int maxAttempts = genericRateLimitService.getMaxRequests(
                "DOCUMENTS_QUERY", GenericRateLimitService.RateLimitDimension.IP);
            int windowMinutes = genericRateLimitService.getWindowMinutes(
                "DOCUMENTS_QUERY", GenericRateLimitService.RateLimitDimension.IP);
            
            String message = String.format(
                "Máximo de solicitudes alcanzado por IP (%d/%d). Intente nuevamente en %d minutos",
                currentAttempts, maxAttempts, minutesLeft);
            
            throw new RateLimitExceededException(
                message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
        }
    }
    
    // 3. Ejecutar lógica de negocio (código existente)
    log.info("Querying documents with criteria: {}, pageable: {}", queryDto, pageable);
    // ... resto del código existente ...
    
    Page<DocumentSearchResponseDto> result = // ... resultado de la consulta ...
    
    // 4. Incrementar contadores SOLO si fue exitoso
    if (token != null && !token.isBlank()) {
        genericRateLimitService.incrementRequest(
            "DOCUMENTS_QUERY", GenericRateLimitService.RateLimitDimension.TOKEN, token);
    }
    if (clientIp != null && !clientIp.isBlank()) {
        genericRateLimitService.incrementRequest(
            "DOCUMENTS_QUERY", GenericRateLimitService.RateLimitDimension.IP, clientIp);
    }
    
    return result;
}

private String extractClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
        return xForwardedFor.split(",")[0].trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
        return xRealIp;
    }
    return request.getRemoteAddr();
}

private String extractToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        return authHeader.substring(7);
    }
    return null;
}
```

### 7. Actualizar interfaz del servicio

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/IInvoiceService.java`

```java
Page<DocumentSearchResponseDto> queryDocuments(
    DocumentQueryDto queryDto, 
    Pageable pageable,
    HttpServletRequest request); // Agregar HttpServletRequest
```

### 8. Crear migración SQL para nuevos parámetros

**Archivo**: `src/main/resources/flyway/V2026010601__add_documents_query_rate_limiting_params.sql`

```sql
-- Parámetros para rate limiting de DOCUMENTS_QUERY
SELECT upsert_param(
    'DOCUMENTS_QUERY_RATE_LIMIT_TOKEN_MAX_REQUESTS',
    '100',
    'Límite máximo de solicitudes por token para consulta de documentos (/api/invoice/documents/query)'
);

SELECT upsert_param(
    'DOCUMENTS_QUERY_RATE_LIMIT_TOKEN_WINDOW_MINUTES',
    '1',
    'Ventana de tiempo en minutos para límite por token (1 = 1 minuto)'
);

SELECT upsert_param(
    'DOCUMENTS_QUERY_RATE_LIMIT_IP_MAX_REQUESTS',
    '500',
    'Límite máximo de solicitudes por IP para consulta de documentos (/api/invoice/documents/query)'
);

SELECT upsert_param(
    'DOCUMENTS_QUERY_RATE_LIMIT_IP_WINDOW_MINUTES',
    '60',
    'Ventana de tiempo en minutos para límite por IP (60 = 1 hora)'
);
```

## 📊 Comparación: Patrón actual vs. Patrón propuesto

| Aspecto | Patrón Actual (AOP) | Patrón Propuesto (Servicio) |
|---------|---------------------|----------------------------|
| **Ubicación** | Aspecto AOP intercepta antes del controlador | Dentro del método del servicio |
| **Verificación** | Antes de ejecutar el método | Antes de ejecutar la lógica de negocio |
| **Incremento** | No se incrementa (solo verifica) | Se incrementa DESPUÉS de éxito |
| **Información en excepción** | Básica (limit, remaining, resetTime) | Rica (attemptsUsed, maxAttempts, minutesUntilNextAttempt, cooldownMinutes) |
| **Respuesta** | Map genérico | DTO estructurado (`RateLimitExceededResponse`) |
| **Parámetros BD** | Formato genérico (`RATE_LIMIT_QUERY_REQ_PER_MIN_TOKEN`) | Formato específico por endpoint (`DOCUMENTS_QUERY_RATE_LIMIT_TOKEN_MAX_REQUESTS`) |
| **Flexibilidad** | Menos flexible (mismo patrón para todos) | Más flexible (configuración por endpoint) |

## ✅ Ventajas del patrón propuesto

1. **Consistencia**: Mismo patrón que el servicio de autenticación
2. **Más información**: La excepción y respuesta incluyen más detalles útiles
3. **Incremento condicional**: Solo incrementa si la operación fue exitosa
4. **Configuración granular**: Parámetros específicos por endpoint
5. **Mejor debugging**: Más información en logs y respuestas

## ⚠️ Consideraciones

1. **Eliminar AOP**: Se debe remover `@RateLimited` y el `RateLimitingAspect` ya no se usará para este endpoint
2. **Mantener compatibilidad**: Los otros endpoints que usan `@RateLimited` seguirán funcionando
3. **Migración de parámetros**: Los parámetros actuales (`RATE_LIMIT_QUERY_REQ_PER_MIN_TOKEN`, etc.) pueden mantenerse para otros endpoints o migrarse al nuevo formato

## 🔄 Pasos de implementación

1. Crear `GenericRateLimitService` e implementación
2. Actualizar `RateLimitExceededException` con nuevos campos
3. Crear `RateLimitExceededResponse` DTO
4. Actualizar `GlobalExceptionHandler` para usar el nuevo DTO
5. Crear migración SQL para nuevos parámetros
6. Actualizar `InvoiceServiceImpl.queryDocuments()` con rate limiting
7. Actualizar `InvoiceController.queryDocuments()` para pasar `HttpServletRequest`
8. Actualizar interfaz `IInvoiceService`
9. Remover `@RateLimited` del controlador (opcional, puede mantenerse para otros endpoints)

## 📝 Notas adicionales

- El servicio genérico puede reutilizarse para otros endpoints en el futuro
- Se mantiene la compatibilidad con el sistema actual de rate limiting (AOP) para otros endpoints
- Los parámetros en BD permiten ajustar límites sin recompilar

