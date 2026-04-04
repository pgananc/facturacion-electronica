# 🚀 API Reference - Subscription Management

## Base URL

```
http://localhost:8480
```

---

## 📋 Endpoints

### 1. Get Current Subscription

**Endpoint:** `GET /api/subscription/{idCompany}/current`

**Description:** Obtiene la suscripción actual de una compañía con todos los detalles del plan, fechas, uso actual y límites.

**Parameters:**

- `idCompany` (path) - ID de la compañía

**Response 200 OK:**

```json
{
  "idSubscription": 1,
  "idCompany": 5,
  "plan": {
    "idPlan": 3,
    "code": "PRO",
    "name": "Plan Profesional",
    "description": "Para empresas establecidas con alto volumen",
    "priceAnnual": 300.0,
    "priceMonthly": 30.0,
    "documentLimitYearly": 2000,
    "userLimit": 10,
    "establishmentLimit": 5,
    "issuancePointLimit": 10,
    "hasApiAccess": true,
    "hasMultiUser": true,
    "hasAdvancedReports": true,
    "hasElectronicInvoice": true,
    "hasCreditNote": true,
    "hasDebitNote": true,
    "hasRetention": true,
    "hasGuide": false,
    "hasPrioritySupport": true,
    "hasCustomBranding": false,
    "hasAutomatedBackups": true,
    "isActive": true,
    "displayOrder": 3
  },
  "status": "ACTIVE",
  "startDate": "2025-01-01T00:00:00",
  "expiresAt": "2026-01-01T00:00:00",
  "currentYearUsage": 456,
  "lastResetDate": "2025-01-01T00:00:00",
  "percentUsed": 22.8,
  "documentsRemaining": 1544,
  "isExpired": false,
  "isLimitReached": false,
  "paymentMethod": "MANUAL",
  "lastPaymentDate": "2025-01-01T00:00:00",
  "lastPaymentAmount": 300.0,
  "paymentReference": "TRANS-2025-001",
  "notes": "Pago por transferencia bancaria"
}
```

**Response 404 Not Found:**

```json
{
  "code": 404,
  "status": "ERROR",
  "message": "No se encontró suscripción activa para la compañía"
}
```

**cURL Example:**

```bash
curl -X GET "http://localhost:8480/api/subscription/5/current" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 2. Get Usage Statistics

**Endpoint:** `GET /api/subscription/{idCompany}/usage`

**Description:** Obtiene estadísticas de uso mensual, anual e histórico de documentos emitidos.

**Parameters:**

- `idCompany` (path) - ID de la compañía

**Response 200 OK:**

```json
{
  "currentMonth": {
    "month": "2025-12",
    "invoices": 35,
    "creditNotes": 5,
    "debitNotes": 0,
    "retentions": 0,
    "guides": 0,
    "total": 40
  },
  "currentYear": {
    "year": 2025,
    "total": 456,
    "limit": 2000,
    "percentUsed": 22.8,
    "remaining": 1544
  },
  "lastMonths": [
    {
      "month": "2025-12",
      "invoices": 35,
      "creditNotes": 5,
      "debitNotes": 0,
      "retentions": 0,
      "guides": 0,
      "total": 40
    },
    {
      "month": "2025-11",
      "invoices": 42,
      "creditNotes": 8,
      "debitNotes": 0,
      "retentions": 0,
      "guides": 0,
      "total": 50
    },
    {
      "month": "2025-10",
      "invoices": 38,
      "creditNotes": 7,
      "debitNotes": 0,
      "retentions": 0,
      "guides": 0,
      "total": 45
    }
    // ... hasta 12 meses
  ]
}
```

**Response 404 Not Found:**

```json
{
  "code": 404,
  "status": "ERROR",
  "message": "No se encontró suscripción activa para la compañía"
}
```

**cURL Example:**

```bash
curl -X GET "http://localhost:8480/api/subscription/5/usage" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 3. Check Can Emit Document

**Endpoint:** `POST /api/subscription/{idCompany}/can-emit`

**Description:** Verifica si una compañía puede emitir un tipo específico de documento. Valida:

- Disponibilidad del tipo de documento en el plan
- Estado de la suscripción (no expirada)
- Límite de documentos no alcanzado

**Parameters:**

- `idCompany` (path) - ID de la compañía

**Request Body:**

```json
{
  "documentType": "INVOICE"
}
```

**Document Types:**

- `INVOICE` - Factura
- `CREDIT_NOTE` - Nota de Crédito
- `DEBIT_NOTE` - Nota de Débito
- `RETENTION` - Retención
- `GUIDE` - Guía de Remisión

**Response 200 OK (Allowed):**

```json
{
  "allowed": true,
  "currentUsage": 456,
  "limit": 2000,
  "remaining": 1544,
  "message": "Puede emitir el documento",
  "reason": "ALLOWED"
}
```

**Response 200 OK (Limit Reached):**

```json
{
  "allowed": false,
  "currentUsage": 2000,
  "limit": 2000,
  "remaining": 0,
  "message": "Ha alcanzado el límite de documentos de su plan",
  "reason": "LIMIT_REACHED"
}
```

**Response 200 OK (Expired Subscription):**

```json
{
  "allowed": false,
  "currentUsage": 456,
  "limit": 2000,
  "remaining": 0,
  "message": "La suscripción ha expirado",
  "reason": "EXPIRED"
}
```

**Response 200 OK (Feature Not Available):**

```json
{
  "allowed": false,
  "currentUsage": 456,
  "limit": 2000,
  "remaining": 1544,
  "message": "El tipo de documento DEBIT_NOTE no está disponible en su plan actual",
  "reason": "FEATURE_NOT_AVAILABLE"
}
```

**Response 200 OK (Super Admin):**

```json
{
  "allowed": true,
  "message": "Super administrador: sin restricciones",
  "reason": "ALLOWED"
}
```

**Response 404 Not Found:**

```json
{
  "code": 404,
  "status": "ERROR",
  "message": "No se encontró suscripción activa para la compañía"
}
```

**cURL Example:**

```bash
curl -X POST "http://localhost:8480/api/subscription/5/can-emit" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "documentType": "INVOICE"
  }'
```

---

### 4. Upgrade Subscription Plan

**Endpoint:** `POST /api/subscription/{idCompany}/upgrade`

**Description:** Actualiza el plan de suscripción de una compañía. **Requiere rol ADMIN o SUPER_ADMIN.**

Actualiza:

- Plan de suscripción
- Fecha de expiración (+1 año desde la fecha actual)
- Información de pago

**Parameters:**

- `idCompany` (path) - ID de la compañía

**Request Body:**

```json
{
  "newPlanCode": "PRO",
  "paymentReference": "TRANS-2025-12345",
  "paymentAmount": 300.0,
  "paymentDate": "2025-12-14T10:30:00",
  "notes": "Upgrade a plan PRO - pago por transferencia bancaria"
}
```

**Request Fields:**

- `newPlanCode` (required) - Código del nuevo plan (FREE, BASIC, PRO, BUSINESS, ENTERPRISE)
- `paymentReference` (optional) - Referencia del pago
- `paymentAmount` (required) - Monto del pago (debe ser > 0)
- `paymentDate` (required) - Fecha y hora del pago
- `notes` (optional) - Notas adicionales

**Response 200 OK:**

```json
{
  "success": true,
  "message": "Plan actualizado exitosamente",
  "subscription": {
    "idSubscription": 1,
    "idCompany": 5,
    "plan": {
      "code": "PRO",
      "name": "Plan Profesional",
      "priceAnnual": 300.0,
      "documentLimitYearly": 2000
      // ... resto del plan
    },
    "status": "ACTIVE",
    "startDate": "2025-01-01T00:00:00",
    "expiresAt": "2026-12-14T10:30:00",
    "currentYearUsage": 456,
    "percentUsed": 22.8,
    "documentsRemaining": 1544,
    "isExpired": false,
    "isLimitReached": false,
    "paymentMethod": "MANUAL",
    "lastPaymentDate": "2025-12-14T10:30:00",
    "lastPaymentAmount": 300.0,
    "paymentReference": "TRANS-2025-12345",
    "notes": "Upgrade a plan PRO - pago por transferencia bancaria"
  }
}
```

**Response 400 Bad Request:**

```json
{
  "code": 400,
  "status": "ERROR",
  "message": "Plan no encontrado o inactivo"
}
```

**Response 403 Forbidden:**

```json
{
  "code": 403,
  "status": "ERROR",
  "message": "No tiene permisos para actualizar el plan"
}
```

**Response 404 Not Found:**

```json
{
  "code": 404,
  "status": "ERROR",
  "message": "No se encontró suscripción activa para la compañía"
}
```

**cURL Example:**

```bash
curl -X POST "http://localhost:8480/api/subscription/5/upgrade" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_OR_SUPER_ADMIN_TOKEN" \
  -d '{
    "newPlanCode": "PRO",
    "paymentReference": "TRANS-2025-12345",
    "paymentAmount": 300.00,
    "paymentDate": "2025-12-14T10:30:00",
    "notes": "Upgrade a plan PRO - pago por transferencia"
  }'
```

---

## 🔑 Authentication

Todos los endpoints requieren autenticación mediante Bearer Token en el header:

```
Authorization: Bearer YOUR_JWT_TOKEN
```

---

## 🎭 Roles

### Endpoints Públicos (cualquier usuario autenticado):

- `GET /api/subscription/{idCompany}/current`
- `GET /api/subscription/{idCompany}/usage`
- `POST /api/subscription/{idCompany}/can-emit`

### Endpoints Administrativos (requieren ADMIN o SUPER_ADMIN):

- `POST /api/subscription/{idCompany}/upgrade`

---

## 🚦 Status Codes

| Code | Description                                 |
| ---- | ------------------------------------------- |
| 200  | Success                                     |
| 400  | Bad Request - Datos inválidos               |
| 403  | Forbidden - Sin permisos o límite alcanzado |
| 404  | Not Found - Suscripción no encontrada       |
| 500  | Internal Server Error                       |

---

## 📊 Subscription Status Values

| Status      | Description                 |
| ----------- | --------------------------- |
| `ACTIVE`    | Suscripción activa y válida |
| `EXPIRED`   | Suscripción expirada        |
| `CANCELLED` | Suscripción cancelada       |
| `SUSPENDED` | Suscripción suspendida      |

---

## 🎯 Can Emit Reason Values

| Reason                  | Description                      |
| ----------------------- | -------------------------------- |
| `ALLOWED`               | Puede emitir el documento        |
| `LIMIT_REACHED`         | Límite de documentos alcanzado   |
| `EXPIRED`               | Suscripción expirada             |
| `FEATURE_NOT_AVAILABLE` | Feature no disponible en el plan |

---

## 📦 Plan Codes

| Code         | Name             | Documents/Year | Price/Year |
| ------------ | ---------------- | -------------- | ---------- |
| `FREE`       | Plan Gratuito    | 50             | $0         |
| `BASIC`      | Plan Básico      | 500            | $120       |
| `PRO`        | Plan Profesional | 2,000          | $300       |
| `BUSINESS`   | Plan Empresarial | 5,000          | $600       |
| `ENTERPRISE` | Plan Corporativo | 999,999        | $1,200     |

---

## 🧪 Testing Examples

### Scenario 1: Check if can emit invoice

```bash
# Check current subscription
curl -X GET "http://localhost:8480/api/subscription/5/current"

# Verify can emit
curl -X POST "http://localhost:8480/api/subscription/5/can-emit" \
  -H "Content-Type: application/json" \
  -d '{"documentType": "INVOICE"}'
```

### Scenario 2: Upgrade to PRO plan

```bash
# Current plan is FREE, want to upgrade to PRO
curl -X POST "http://localhost:8480/api/subscription/5/upgrade" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{
    "newPlanCode": "PRO",
    "paymentAmount": 300.00,
    "paymentDate": "2025-12-14T10:00:00",
    "paymentReference": "TRANS-001"
  }'
```

### Scenario 3: Monitor monthly usage

```bash
# Get usage statistics
curl -X GET "http://localhost:8480/api/subscription/5/usage"

# Extract current month usage
# Current month shows: 35 invoices + 5 credit notes = 40 documents
```

---

## 🐛 Common Errors

### Error: "No se encontró suscripción activa"

**Cause:** La compañía no tiene una suscripción activa  
**Solution:** Asegúrese de que la compañía fue migrada correctamente o crear una suscripción manualmente

### Error: "Ha alcanzado el límite de documentos"

**Cause:** La compañía alcanzó el límite anual de documentos  
**Solution:** Actualizar a un plan superior con más documentos

### Error: "No tiene permisos para actualizar el plan"

**Cause:** El usuario no tiene rol ADMIN o SUPER_ADMIN  
**Solution:** Usar un token de usuario con permisos administrativos

### Error: "Plan no encontrado o inactivo"

**Cause:** El código del plan no existe o está inactivo  
**Solution:** Usar uno de los códigos válidos: FREE, BASIC, PRO, BUSINESS, ENTERPRISE

---

## 📝 Notes

1. Super Admin users bypass all subscription limits
2. Usage counters are updated automatically via database triggers
3. Subscription expiration is calculated as start date + 1 year
4. Only authorized documents (with `id_authorization_sri`) count towards limits
5. All dates use ISO 8601 format: `YYYY-MM-DDTHH:mm:ss`
6. Prices are in USD

---

**Version:** 1.0.0  
**Last Updated:** December 14, 2025
