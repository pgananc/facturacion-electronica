# 📚 API Reference - Subscription Management

## Autenticación

Todos los endpoints requieren:

- Bearer Token válido
- Rol: `SUPER_ADMIN`
- Feature: `SUBSCRIPTION_MANAGEMENT` activo

```bash
# Header requerido
Authorization: Bearer {token}
```

---

## Endpoints

### 1. Listar Todas las Suscripciones

```http
GET /api/subscriptions
```

**Query Parameters:**

| Parámetro  | Tipo    | Requerido | Descripción                                                        |
| ---------- | ------- | --------- | ------------------------------------------------------------------ |
| `status`   | string  | No        | Filtrar por estado: `ACTIVE`, `EXPIRED`, `CANCELLED`, `SUSPENDED`  |
| `planCode` | string  | No        | Filtrar por plan: `FREE`, `BASIC`, `PRO`, `BUSINESS`, `ENTERPRISE` |
| `page`     | integer | No        | Número de página (default: 0)                                      |
| `size`     | integer | No        | Tamaño de página (default: 20)                                     |

**Ejemplo de Request:**

```bash
curl -X GET "http://localhost:8480/api/subscriptions?status=ACTIVE&planCode=FREE&page=0&size=10" \
  -H "Authorization: Bearer {token}"
```

**Response 200 OK:**

```json
{
  "content": [
    {
      "idSubscription": 1,
      "idCompany": 123,
      "companyName": null,
      "plan": {
        "idPlan": 1,
        "code": "FREE",
        "name": "Plan Gratuito",
        "documentLimitYearly": 50,
        "priceAnnual": 0.0,
        "priceMonthly": 0.0
      },
      "subscriptionStatus": "ACTIVE",
      "startDate": "2025-01-01T00:00:00",
      "expiresAt": "2026-01-01T00:00:00",
      "currentYearUsage": 15,
      "percentUsed": 30.0,
      "documentsRemaining": 35,
      "isExpired": false,
      "isLimitReached": false,
      "cancelledAt": null,
      "cancelReason": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 150,
  "totalPages": 15,
  "last": false,
  "first": true
}
```

---

### 2. Ver Suscripción de una Empresa

```http
GET /api/subscriptions/company/{idCompany}
```

**Path Parameters:**

| Parámetro   | Tipo | Descripción      |
| ----------- | ---- | ---------------- |
| `idCompany` | long | ID de la empresa |

**Ejemplo de Request:**

```bash
curl -X GET "http://localhost:8480/api/subscriptions/company/123" \
  -H "Authorization: Bearer {token}"
```

**Response 200 OK:**

```json
{
  "idSubscription": 1,
  "idCompany": 123,
  "plan": {
    "idPlan": 1,
    "code": "FREE",
    "name": "Plan Gratuito",
    "description": "Plan básico sin costo",
    "priceAnnual": 0.0,
    "priceMonthly": 0.0,
    "documentLimitYearly": 50,
    "userLimit": 1,
    "establishmentLimit": 1,
    "issuancePointLimit": 1,
    "hasApiAccess": false,
    "hasMultiUser": false,
    "hasElectronicInvoice": true,
    "hasCreditNote": true
  },
  "subscriptionStatus": "ACTIVE",
  "startDate": "2025-01-01T00:00:00",
  "expiresAt": "2026-01-01T00:00:00",
  "currentYearUsage": 15,
  "percentUsed": 30.0,
  "documentsRemaining": 35,
  "isExpired": false,
  "isLimitReached": false
}
```

---

### 3. Cambiar Plan de Suscripción

```http
POST /api/subscriptions/company/{idCompany}/upgrade
```

**Path Parameters:**

| Parámetro   | Tipo | Descripción      |
| ----------- | ---- | ---------------- |
| `idCompany` | long | ID de la empresa |

**Request Body:**

```json
{
  "newPlanCode": "BASIC",
  "paymentDate": "2025-12-17T18:00:00",
  "paymentAmount": 120.0,
  "paymentReference": "PAGO-2025-001",
  "notes": "Upgrade a plan BASIC por compra anual"
}
```

**Ejemplo de Request:**

```bash
curl -X POST "http://localhost:8480/api/subscriptions/company/123/upgrade" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "newPlanCode": "BASIC",
    "paymentDate": "2025-12-17T18:00:00",
    "paymentAmount": 120.00,
    "paymentReference": "PAGO-2025-001",
    "notes": "Upgrade a plan BASIC"
  }'
```

**Response 200 OK:**

```json
{
  "success": true,
  "message": "Plan actualizado exitosamente",
  "subscription": {
    "idSubscription": 1,
    "idCompany": 123,
    "plan": {
      "code": "BASIC",
      "name": "Plan Básico"
    },
    "subscriptionStatus": "ACTIVE",
    "expiresAt": "2026-12-17T18:00:00"
  }
}
```

---

### 4. Extender Vigencia de Suscripción

```http
POST /api/subscriptions/company/{idCompany}/extend
```

**Path Parameters:**

| Parámetro   | Tipo | Descripción      |
| ----------- | ---- | ---------------- |
| `idCompany` | long | ID de la empresa |

**Request Body:**

```json
{
  "extensionMonths": 12,
  "paymentDate": "2025-12-17T18:00:00",
  "paymentAmount": 120.0,
  "paymentReference": "RENOVACION-2025-001",
  "notes": "Renovación anual"
}
```

**Ejemplo de Request:**

```bash
curl -X POST "http://localhost:8480/api/subscriptions/company/123/extend" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "extensionMonths": 12,
    "paymentDate": "2025-12-17T18:00:00",
    "paymentAmount": 120.00,
    "paymentReference": "RENOVACION-2025-001",
    "notes": "Renovación anual"
  }'
```

**Response 200 OK:**

```json
{
  "success": true,
  "message": "Suscripción extendida exitosamente",
  "previousExpiryDate": "2026-01-01T00:00:00",
  "newExpiryDate": "2027-01-01T00:00:00",
  "extensionMonths": 12,
  "subscription": {
    "idSubscription": 1,
    "idCompany": 123,
    "subscriptionStatus": "ACTIVE",
    "expiresAt": "2027-01-01T00:00:00"
  }
}
```

---

### 5. Cancelar Suscripción

```http
DELETE /api/subscriptions/company/{idCompany}/cancel
```

**Path Parameters:**

| Parámetro   | Tipo | Descripción      |
| ----------- | ---- | ---------------- |
| `idCompany` | long | ID de la empresa |

**Request Body:**

```json
{
  "reason": "PAYMENT_FAILURE",
  "notes": "Cliente no renovó el pago",
  "cancelImmediately": true
}
```

**Valores de `reason`:**

- `PAYMENT_FAILURE`: Fallo en el pago
- `USER_REQUEST`: Solicitud del usuario
- `VIOLATION`: Violación de términos
- `OTHER`: Otra razón

**Ejemplo de Request:**

```bash
curl -X DELETE "http://localhost:8480/api/subscriptions/company/123/cancel" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "PAYMENT_FAILURE",
    "notes": "Cliente no renovó el pago",
    "cancelImmediately": true
  }'
```

**Response 200 OK:**

```json
{
  "success": true,
  "message": "Suscripción cancelada exitosamente",
  "cancelledAt": "2025-12-17T18:00:00",
  "cancelReason": "PAYMENT_FAILURE",
  "subscription": {
    "idSubscription": 1,
    "idCompany": 123,
    "subscriptionStatus": "CANCELLED",
    "cancelledAt": "2025-12-17T18:00:00",
    "cancelReason": "PAYMENT_FAILURE"
  }
}
```

---

## Códigos de Error

| Código | Descripción                                                  |
| ------ | ------------------------------------------------------------ |
| 401    | No autenticado - Token inválido o faltante                   |
| 403    | No autorizado - Usuario no es SUPER_ADMIN o feature inactivo |
| 404    | Recurso no encontrado - Empresa o plan no existe             |
| 400    | Bad Request - Datos inválidos o validación fallida           |
| 500    | Error interno del servidor                                   |

**Ejemplo de Error 403:**

```json
{
  "timestamp": "2025-12-17T18:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/subscriptions"
}
```

---

## Notas Adicionales

### Paginación

Los endpoints que retornan listas utilizan paginación estándar de Spring Data:

```bash
# Obtener página 2 con 50 elementos
GET /api/subscriptions?page=1&size=50

# Ordenar por fecha de creación descendente
GET /api/subscriptions?page=0&size=20&sort=creationDate,desc
```

### Filtros Combinados

Puedes combinar múltiples filtros:

```bash
GET /api/subscriptions?status=ACTIVE&planCode=PRO&page=0&size=20
```

### Testing con cURL

Para facilitar el testing, puedes exportar el token:

```bash
export TOKEN="tu_token_jwt_aqui"

# Listar suscripciones
curl -X GET "http://localhost:8480/api/subscriptions" \
  -H "Authorization: Bearer $TOKEN"
```

---

**Última actualización:** 17 de Diciembre, 2025  
**Versión API:** 1.0
