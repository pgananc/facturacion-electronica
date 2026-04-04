# 📋 Análisis: GET /api/subscription/{idCompany}/current

## 🔍 cURL Analizado

```bash
curl -X GET http://localhost:8480/api/subscription/1/current \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMCIsInJvbGVzIjpbIm1lbWJlciJdLCJ1c2VyTmFtZSI6IjAyMDQwNTA2MDciLCJjb21wYW55RXN0YWJsaXNobWVudHMiOlt7ImlkQ29tcGFueSI6MSwiZXN0YWJsaXNobWVudHMiOltdfV0sImlhdCI6MTc2NzY1MTc2MCwiZXhwIjoxNzY3NjUzNTYwfQ.OE1tDjOKj2Ko4kYPWxTO_sonq4U5lMemBdcQcWAKbL4"
```

### Detalles de la Llamada

- **Endpoint:** `GET /api/subscription/{idCompany}/current`
- **URL Base:** `http://localhost:8480`
- **Path Variable:** `{idCompany} = 1`
- **Header:** `Authorization: Bearer <JWT_TOKEN>`
- **Token JWT:** Token del usuario autenticado (user ID: 10, company ID: 1)

---

## 📊 Flujo de Ejecución

### 1. **Controller: SubscriptionController.getCurrentSubscription()**

**Línea 45-46:** Recibe el request

```java
@GetMapping("/{idCompany}/current")
public ResponseEntity<?> getCurrentSubscription(@PathVariable("idCompany") Long idCompany)
```

**Líneas 47-54:** Log de REQUEST (usando `setLogs` de `ebill-utils`)

```java
setLogs(
    EventLog.builder()
        .service("SubscriptionController")
        .method("getCurrentSubscription")
        .information("Obteniendo suscripción actual de company: 1"),
    EventType.REQUEST,
    Level.LEVEL_001);
```

**Log esperado:**

```
INFO - [REQUEST] SubscriptionController.getCurrentSubscription - Obteniendo suscripción actual de company: 1
```

**Línea 57:** Llama al servicio

```java
SubscriptionResponseDto response = subscriptionService.getCurrentSubscription(idCompany);
```

**Líneas 59-65:** Log de RESPONSE (si es exitoso)

```java
setLogs(
    EventLog.builder()
        .service("SubscriptionController")
        .method("getCurrentSubscription")
        .information("Suscripción obtenida: " + response.getPlan().getName()),
    EventType.RESPONSE,
    Level.LEVEL_001);
```

**Log esperado:**

```
INFO - [RESPONSE] SubscriptionController.getCurrentSubscription - Suscripción obtenida: Plan Profesional
```

**Línea 67:** Retorna respuesta HTTP 200 OK

```java
return new ResponseEntity<>(response, HttpStatus.OK);
```

---

### 2. **Service: SubscriptionServiceImpl.getCurrentSubscription()**

**Línea 62:** Log de inicio

```java
log.info("📋 Obteniendo suscripción actual para company: {}", idCompany);
```

**Log esperado:**

```
INFO - 📋 Obteniendo suscripción actual para company: 1
```

**Líneas 64-70:** Busca suscripción activa en BD

```java
CompanySubscription subscription =
    companySubscriptionRepo
        .findActiveByIdCompany(idCompany)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No se encontró suscripción activa para la compañía"));
```

**Línea 72:** Mapea a DTO

```java
return mapToResponseDto(subscription);
```

---

### 3. **Método Privado: mapToResponseDto()**

**Líneas 386-443:** Construye el DTO completo

**Construye `SubscriptionPlanDto`:**

- `idPlan`, `code`, `name`, `description`
- `priceAnnual`, `priceMonthly`
- `documentLimitYearly`, `userLimit`, `establishmentLimit`, `issuancePointLimit`
- Todas las features booleanas (`hasApiAccess`, `hasMultiUser`, etc.)
- `isActive`, `displayOrder`

**Calcula campos derivados:**

- `percentUsed`: Porcentaje de documentos usados (currentUsage / limit \* 100)
- `documentsRemaining`: Documentos restantes (limit - currentUsage)
- `isExpired`: Si `expiresAt < ahora`
- `isLimitReached`: Si `currentUsage >= limit`

**Construye `SubscriptionResponseDto`:**

- `idSubscription`, `idCompany`
- `plan` (SubscriptionPlanDto completo)
- `subscriptionStatus` ("ACTIVE", "EXPIRED", "CANCELLED")
- `startDate`, `expiresAt`
- `currentYearUsage`, `lastResetDate`
- `percentUsed`, `documentsRemaining`
- `isExpired`, `isLimitReached`
- `paymentMethod`, `lastPaymentDate`, `lastPaymentAmount`, `paymentReference`, `notes`

---

## 📝 Logs que se Imprimen

### ✅ Caso Exitoso (200 OK)

```
INFO - [REQUEST] SubscriptionController.getCurrentSubscription - Obteniendo suscripción actual de company: 1
INFO - 📋 Obteniendo suscripción actual para company: 1
INFO - [RESPONSE] SubscriptionController.getCurrentSubscription - Suscripción obtenida: Plan Profesional
```

### ❌ Caso Error 404 (No encontrada)

**Línea 70:** Se lanza `IllegalArgumentException`

**Línea 70:** Log de error

```java
log.error("❌ Error obteniendo suscripción: {}", e.getMessage());
```

**Log esperado:**

```
ERROR - ❌ Error obteniendo suscripción: No se encontró suscripción activa para la compañía
```

**Líneas 72-77:** Retorna error 404

```java
Map<String, Object> errorResponse = new HashMap<>();
errorResponse.put("code", 404);
errorResponse.put("status", "ERROR");
errorResponse.put("message", e.getMessage());

return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
```

### ❌ Caso Error 500 (Error inesperado)

**Línea 79:** Log de error con stack trace

```java
log.error("❌ Error inesperado obteniendo suscripción: {}", e.getMessage(), e);
```

**Líneas 81-86:** Retorna error 500

```java
Map<String, Object> errorResponse = new HashMap<>();
errorResponse.put("code", 500);
errorResponse.put("status", "ERROR");
errorResponse.put("message", "Error interno al obtener la suscripción");

return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
```

---

## 📦 Response JSON (200 OK)

```json
{
  "idSubscription": 1,
  "idCompany": 1,
  "plan": {
    "idPlan": 3,
    "code": "PROFESSIONAL",
    "name": "Plan Profesional",
    "description": "Plan profesional para empresas en crecimiento",
    "priceAnnual": 3600.0,
    "priceMonthly": 300.0,
    "documentLimitYearly": 5000,
    "userLimit": 10,
    "establishmentLimit": 5,
    "issuancePointLimit": 20,
    "hasApiAccess": true,
    "hasMultiUser": true,
    "hasAdvancedReports": true,
    "hasElectronicInvoice": true,
    "hasCreditNote": true,
    "hasDebitNote": true,
    "hasRetention": true,
    "hasGuide": true,
    "hasPrioritySupport": true,
    "hasCustomBranding": false,
    "hasAutomatedBackups": true,
    "isActive": true,
    "displayOrder": 3
  },
  "subscriptionStatus": "ACTIVE",
  "startDate": "2025-01-01T00:00:00",
  "expiresAt": "2026-01-01T00:00:00",
  "currentYearUsage": 456,
  "lastResetDate": "2025-01-01T00:00:00",
  "percentUsed": 9.12,
  "documentsRemaining": 4544,
  "isExpired": false,
  "isLimitReached": false,
  "paymentMethod": "MANUAL",
  "lastPaymentDate": "2025-01-01T00:00:00",
  "lastPaymentAmount": 3600.0,
  "paymentReference": "TRANS-2025-001",
  "notes": "Pago por transferencia bancaria"
}
```

---

## ❌ Response JSON (404 Not Found)

```json
{
  "code": 404,
  "status": "ERROR",
  "message": "No se encontró suscripción activa para la compañía"
}
```

---

## ❌ Response JSON (500 Internal Server Error)

```json
{
  "code": 500,
  "status": "ERROR",
  "message": "Error interno al obtener la suscripción"
}
```

---

## 🔐 Autenticación

El endpoint **requiere autenticación JWT** porque:

1. No está en la lista de `permitAll()` en `SecurityConfig`
2. El filtro JWT de `ebill-auth-util` valida el token contra Redis
3. El token debe tener formato: `Authorization: Bearer <token>`

**Token usado en el cURL:**

- **User ID:** 10
- **Username:** 0204050607
- **Roles:** ["member"]
- **Company ID:** 1
- **Expiración:** 1767653560 (timestamp Unix)

---

## 📊 Resumen de Logs

| Momento              | Log                                                  | Nivel | Ubicación           |
| -------------------- | ---------------------------------------------------- | ----- | ------------------- |
| **Request recibido** | `Obteniendo suscripción actual de company: 1`        | INFO  | Controller línea 52 |
| **Inicio servicio**  | `📋 Obteniendo suscripción actual para company: 1`   | INFO  | Service línea 62    |
| **Response exitoso** | `Suscripción obtenida: Plan Profesional`             | INFO  | Controller línea 63 |
| **Error 404**        | `❌ Error obteniendo suscripción: No se encontró...` | ERROR | Controller línea 70 |
| **Error 500**        | `❌ Error inesperado obteniendo suscripción: ...`    | ERROR | Controller línea 79 |

---

## ✅ Validaciones del Endpoint

1. **Autenticación JWT:** Validada por `ebill-auth-util` antes de llegar al controller
2. **Suscripción activa:** Busca solo suscripciones con `status = 'ACTIVE'`
3. **Cálculos automáticos:**
   - `percentUsed`: Calculado como `(currentUsage / limit) * 100`
   - `documentsRemaining`: `max(0, limit - currentUsage)`
   - `isExpired`: Compara `expiresAt` con fecha actual
   - `isLimitReached`: Compara `currentUsage >= limit`

---

## 🎯 Observaciones del cURL

✅ **Correcto:**

- Header `Authorization: Bearer` con token válido
- Path variable `{idCompany} = 1` correcto
- Método GET correcto

⚠️ **Nota:**

- El token JWT tiene expiración: `exp: 1767653560`
- Si el token expira, recibirás **401 Unauthorized**
- El token debe estar almacenado en Redis para ser válido

---

## 🔄 Flujo Completo Visual

```
1. Request HTTP
   ↓
2. Filtro JWT (ebill-auth-util) → Valida token contra Redis
   ↓
3. Controller.getCurrentSubscription() → Log REQUEST
   ↓
4. Service.getCurrentSubscription() → Log INFO
   ↓
5. Repository.findActiveByIdCompany() → Query BD
   ↓
6. mapToResponseDto() → Construye DTO completo
   ↓
7. Controller → Log RESPONSE
   ↓
8. Response HTTP 200 OK con JSON
```

---

## 📚 Referencias

- **Controller:** `SubscriptionController.java` líneas 45-88
- **Service:** `SubscriptionServiceImpl.java` líneas 61-73
- **Mapper:** `SubscriptionServiceImpl.mapToResponseDto()` líneas 386-443
- **DTO:** `SubscriptionResponseDto.java`
