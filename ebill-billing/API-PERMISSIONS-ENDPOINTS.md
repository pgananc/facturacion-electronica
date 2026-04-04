# 📚 API Reference - Permissions Endpoints

## Base URL

```
http://localhost:8480
```

---

## 🔐 Autenticación

Todos los endpoints requieren autenticación mediante Bearer Token:

```
Authorization: Bearer YOUR_JWT_TOKEN
```

---

## 📋 Endpoints

### 1. GET /api/permissions/features/plans

**Descripción:** Obtiene las features disponibles en cada plan de suscripción activo.

**Acceso:** Cualquier usuario autenticado con un rol

**cURL:**

```bash
curl -X GET "http://localhost:8480/api/permissions/features/plans" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Response 200 OK:**

```json
[
  {
    "planCode": "FREE",
    "planName": "Plan Gratuito",
    "features": {
      "hasApiAccess": false,
      "hasMultiUser": false,
      "hasAdvancedReports": false,
      "hasElectronicInvoice": true,
      "hasCreditNote": true,
      "hasDebitNote": false,
      "hasRetention": false,
      "hasGuide": false,
      "hasPrioritySupport": false,
      "hasCustomBranding": false,
      "hasAutomatedBackups": false
    },
    "featureCodes": [
      "CLIENT_CREATE",
      "CARRIER_CREATE",
      "PRODUCT_MANAGEMENT",
      "COMPANY_SETTINGS",
      "ESTABLISHMENT_MANAGEMENT",
      "DRAFT_MANAGEMENT",
      "INVOICE_POINT_MANAGEMENT",
      "REPORT_PENDING_INVOICES",
      "INVOICE_CREATE",
      "CREDIT_NOTE_CREATE"
    ]
  },
  {
    "planCode": "BASIC",
    "planName": "Plan Básico",
    "features": {
      "hasApiAccess": false,
      "hasMultiUser": true,
      "hasAdvancedReports": false,
      "hasElectronicInvoice": true,
      "hasCreditNote": true,
      "hasDebitNote": true,
      "hasRetention": false,
      "hasGuide": false,
      "hasPrioritySupport": false,
      "hasCustomBranding": false,
      "hasAutomatedBackups": true
    },
    "featureCodes": [
      "CLIENT_CREATE",
      "CARRIER_CREATE",
      "PRODUCT_MANAGEMENT",
      "COMPANY_SETTINGS",
      "ESTABLISHMENT_MANAGEMENT",
      "DRAFT_MANAGEMENT",
      "INVOICE_POINT_MANAGEMENT",
      "REPORT_PENDING_INVOICES",
      "INVOICE_CREATE",
      "CREDIT_NOTE_CREATE",
      "DEBIT_NOTE_CREATE",
      "COMPANY_USER_MANAGEMENT",
      "USER_MANAGEMENT"
    ]
  },
  {
    "planCode": "PROFESSIONAL",
    "planName": "Plan Profesional",
    "features": {
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
      "hasAutomatedBackups": true
    },
    "featureCodes": [
      "CLIENT_CREATE",
      "CARRIER_CREATE",
      "PRODUCT_MANAGEMENT",
      "COMPANY_SETTINGS",
      "ESTABLISHMENT_MANAGEMENT",
      "DRAFT_MANAGEMENT",
      "INVOICE_POINT_MANAGEMENT",
      "REPORT_PENDING_INVOICES",
      "INVOICE_CREATE",
      "CREDIT_NOTE_CREATE",
      "DEBIT_NOTE_CREATE",
      "API_ACCESS",
      "COMPANY_USER_MANAGEMENT",
      "USER_MANAGEMENT",
      "REPORT_SALES_DASHBOARD",
      "REPORT_BRANCH_COMPARISON",
      "REPORT_SALES_BY_PERIOD",
      "REPORT_SALES_BY_CLIENT",
      "REPORT_SALES_BY_PRODUCT",
      "REPORT_CREDIT_NOTES",
      "REPORT_PAYMENT_METHODS"
    ]
  }
]
```

**Nota:** El campo `featureCodes` contiene los códigos de features habilitadas para el plan, calculados dinámicamente a partir de los booleanos en `features`. Esto elimina la necesidad de hardcodear el mapeo en el frontend. Las features siempre disponibles (como `CLIENT_CREATE`, `PRODUCT_MANAGEMENT`, etc.) se incluyen automáticamente en todos los planes.

**Response 401 Unauthorized:**

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Usuario no autenticado o sin roles asignados"
}
```

---

### 2. PUT /api/permissions/plans/bulk

**Descripción:** Actualiza las features de múltiples planes de suscripción en una operación masiva.

**Acceso:** Solo SUPER_ADMIN

**cURL:**

```bash
curl -X PUT "http://localhost:8480/api/permissions/plans/bulk" \
  -H "Authorization: Bearer YOUR_SUPER_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "updates": [
      {
        "planCode": "FREE",
        "features": {
          "hasApiAccess": false,
          "hasMultiUser": false,
          "hasAdvancedReports": false,
          "hasElectronicInvoice": true,
          "hasCreditNote": true,
          "hasDebitNote": false,
          "hasRetention": false,
          "hasGuide": false,
          "hasPrioritySupport": false,
          "hasCustomBranding": false,
          "hasAutomatedBackups": false
        }
      },
      {
        "planCode": "BASIC",
        "features": {
          "hasApiAccess": true,
          "hasMultiUser": true,
          "hasAdvancedReports": false,
          "hasElectronicInvoice": true,
          "hasCreditNote": true,
          "hasDebitNote": true,
          "hasRetention": false,
          "hasGuide": false,
          "hasPrioritySupport": false,
          "hasCustomBranding": false,
          "hasAutomatedBackups": true
        }
      }
    ]
  }'
```

**Request Body:**

```json
{
  "updates": [
    {
      "planCode": "FREE",
      "features": {
        "hasApiAccess": false,
        "hasMultiUser": false,
        "hasAdvancedReports": false,
        "hasElectronicInvoice": true,
        "hasCreditNote": true,
        "hasDebitNote": false,
        "hasRetention": false,
        "hasGuide": false,
        "hasPrioritySupport": false,
        "hasCustomBranding": false,
        "hasAutomatedBackups": false
      }
    },
    {
      "planCode": "BASIC",
      "features": {
        "hasApiAccess": true,
        "hasMultiUser": true,
        "hasAdvancedReports": false,
        "hasElectronicInvoice": true,
        "hasCreditNote": true,
        "hasDebitNote": true,
        "hasRetention": false,
        "hasGuide": false,
        "hasPrioritySupport": false,
        "hasCustomBranding": false,
        "hasAutomatedBackups": true
      }
    }
  ]
}
```

**Response 200 OK (Éxito total):**

```json
{
  "success": true,
  "message": "Se actualizaron exitosamente 2 plan(es)",
  "totalUpdated": 2,
  "totalFailed": 0,
  "results": [
    {
      "identifier": "FREE",
      "success": true,
      "message": "Plan actualizado exitosamente",
      "error": null
    },
    {
      "identifier": "BASIC",
      "success": true,
      "message": "Plan actualizado exitosamente",
      "error": null
    }
  ],
  "timestamp": "2025-01-15T10:30:00"
}
```

**Response 200 OK (Con errores parciales):**

```json
{
  "success": false,
  "message": "Se actualizaron 1 plan(es) exitosamente, 1 fallaron",
  "totalUpdated": 1,
  "totalFailed": 1,
  "results": [
    {
      "identifier": "FREE",
      "success": true,
      "message": "Plan actualizado exitosamente",
      "error": null
    },
    {
      "identifier": "INVALID_PLAN",
      "success": false,
      "message": "Error al actualizar plan",
      "error": "Plan con código 'INVALID_PLAN' no encontrado"
    }
  ],
  "timestamp": "2025-01-15T10:30:00"
}
```

**Response 403 Forbidden:**

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Se requiere rol SUPER_ADMIN para realizar esta operación"
}
```

**Response 400 Bad Request (Validación):**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Datos inválidos. Verifica la información",
  "timestamp": "2025-01-15T10:30:00",
  "path": "/api/permissions/plans/bulk",
  "errors": [
    {
      "field": "updates",
      "message": "La lista de actualizaciones no puede estar vacía"
    }
  ]
}
```

---

## 🚦 Status Codes

| Code | Description                                     |
| ---- | ----------------------------------------------- |
| 200  | Success                                         |
| 400  | Bad Request - Datos inválidos o validación      |
| 401  | Unauthorized - Usuario no autenticado           |
| 403  | Forbidden - Sin permisos (requiere SUPER_ADMIN) |
| 500  | Internal Server Error                           |

---

## 📝 Notas

1. **Autenticación:** Todos los endpoints requieren un token JWT válido en el header `Authorization`.

2. **Permisos:**

   - `GET /api/permissions/features/plans` - Cualquier usuario autenticado
   - `PUT /api/permissions/plans/bulk` - Solo SUPER_ADMIN

3. **Validaciones:**

   - Los endpoints PUT validan que la lista de actualizaciones no esté vacía
   - Los códigos de plan deben existir en la base de datos
   - Solo se actualizan los campos que se envían en el request (parcial update)

4. **Operaciones Bulk:**
   - Las operaciones bulk procesan todas las actualizaciones incluso si algunas fallan
   - El campo `success` indica si todas las operaciones fueron exitosas
   - Cada resultado individual incluye su estado de éxito/fallo

5. **Feature Codes:**
   - El campo `featureCodes` en `GET /api/permissions/features/plans` se calcula dinámicamente a partir de los booleanos en `features`
   - **Features siempre disponibles** (incluidas en todos los planes):
     - `CLIENT_CREATE`
     - `CARRIER_CREATE`
     - `PRODUCT_MANAGEMENT`
     - `COMPANY_SETTINGS`
     - `ESTABLISHMENT_MANAGEMENT`
     - `DRAFT_MANAGEMENT`
     - `INVOICE_POINT_MANAGEMENT`
     - `REPORT_PENDING_INVOICES`
   - **Mapeo de booleanos a feature codes:**
     - `hasElectronicInvoice: true` → `INVOICE_CREATE`
     - `hasCreditNote: true` → `CREDIT_NOTE_CREATE`
     - `hasDebitNote: true` → `DEBIT_NOTE_CREATE`
     - `hasGuide: true` → `DELIVERY_GUIDE_CREATE`
     - `hasAdvancedReports: true` → `REPORT_SALES_DASHBOARD`, `REPORT_BRANCH_COMPARISON`, `REPORT_SALES_BY_PERIOD`, `REPORT_SALES_BY_CLIENT`, `REPORT_SALES_BY_PRODUCT`, `REPORT_CREDIT_NOTES`, `REPORT_PAYMENT_METHODS`
     - `hasApiAccess: true` → `API_ACCESS`
     - `hasMultiUser: true` → `COMPANY_USER_MANAGEMENT`, `USER_MANAGEMENT`
   - El frontend puede usar directamente `featureCodes` sin necesidad de hardcodear el mapeo

---

## 🔍 Ejemplos de Uso

### Actualizar solo algunas features de un plan

```json
{
  "updates": [
    {
      "planCode": "BASIC",
      "features": {
        "hasApiAccess": true,
        "hasAutomatedBackups": true
      }
    }
  ]
}
```

Solo se actualizarán `hasApiAccess` y `hasAutomatedBackups`, el resto de features permanecerán sin cambios.
