# 🎯 Feature: SUBSCRIPTION_MANAGEMENT

## 📋 Especificación del Feature

### Información General

- **Código:** `SUBSCRIPTION_MANAGEMENT`
- **Nombre:** Gestión de Suscripciones
- **Descripción:** Permite a los super administradores gestionar las suscripciones de todas las empresas del sistema
- **Roles Permitidos:** Solo `SUPER_ADMIN` (id_role = 1)
- **Planes Requeridos:** Todos (`FREE`, `BASIC`, `PROFESSIONAL`, `ENTERPRISE`)

---

## 🚀 Endpoints API

### 1. GET /api/subscriptions

**Listar todas las suscripciones del sistema**

**Autorización:** Solo SUPER_ADMIN

**Query Parameters:**

- `status` (opcional): Filtrar por estado (`ACTIVE`, `EXPIRED`, `CANCELLED`, `SUSPENDED`)
- `planCode` (opcional): Filtrar por código de plan (`FREE`, `BASIC`, `PRO`, `BUSINESS`, `ENTERPRISE`)
- `page` (opcional): Número de página (default: 0)
- `size` (opcional): Tamaño de página (default: 20)

**Response:**

```json
{
  "content": [
    {
      "idSubscription": 1,
      "idCompany": 123,
      "companyName": "Empresa XYZ S.A.",
      "plan": {
        "code": "FREE",
        "name": "Plan Gratuito",
        "documentLimitYearly": 50
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
  ],
  "totalElements": 150,
  "totalPages": 8,
  "currentPage": 0,
  "pageSize": 20
}
```

---

### 2. GET /api/subscriptions/company/{idCompany}

**Ver suscripción de una empresa específica**

**Autorización:** Solo SUPER_ADMIN

**Path Parameters:**

- `idCompany` (required): ID de la empresa

**Response:**

```json
{
  "idSubscription": 1,
  "idCompany": 123,
  "plan": {
    "idPlan": 1,
    "code": "FREE",
    "name": "Plan Gratuito",
    "description": "Plan básico sin costo para emprendedores",
    "priceAnnual": 0.0,
    "priceMonthly": 0.0,
    "documentLimitYearly": 50,
    "userLimit": 1,
    "establishmentLimit": 1,
    "issuancePointLimit": 1,
    "hasApiAccess": false,
    "hasMultiUser": false,
    "hasAdvancedReports": false,
    "hasElectronicInvoice": true,
    "hasCreditNote": true,
    "hasPrioritySupport": false
  },
  "subscriptionStatus": "ACTIVE",
  "startDate": "2025-01-01T00:00:00",
  "expiresAt": "2026-01-01T00:00:00",
  "currentYearUsage": 15,
  "lastResetDate": "2025-01-01T00:00:00",
  "percentUsed": 30.0,
  "documentsRemaining": 35,
  "isExpired": false,
  "isLimitReached": false,
  "paymentMethod": "FREE",
  "lastPaymentDate": null,
  "lastPaymentAmount": null,
  "notes": "Migración automática a plan FREE"
}
```

---

### 3. POST /api/subscriptions/company/{idCompany}/upgrade

**Cambiar plan de suscripción de una empresa**

**Autorización:** Solo SUPER_ADMIN

**Path Parameters:**

- `idCompany` (required): ID de la empresa

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

**Response:**

```json
{
  "success": true,
  "message": "Plan actualizado exitosamente",
  "subscription": {
    "idSubscription": 1,
    "idCompany": 123,
    "plan": {
      "code": "BASIC",
      "name": "Plan Básico",
      "documentLimitYearly": 500
    },
    "subscriptionStatus": "ACTIVE",
    "expiresAt": "2026-12-17T18:00:00",
    "currentYearUsage": 15,
    "percentUsed": 3.0,
    "documentsRemaining": 485
  }
}
```

---

### 4. POST /api/subscriptions/company/{idCompany}/extend

**Extender vigencia de una suscripción**

**Autorización:** Solo SUPER_ADMIN

**Path Parameters:**

- `idCompany` (required): ID de la empresa

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

**Response:**

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

### 5. DELETE /api/subscriptions/company/{idCompany}/cancel

**Cancelar suscripción de una empresa**

**Autorización:** Solo SUPER_ADMIN

**Path Parameters:**

- `idCompany` (required): ID de la empresa

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

**Response:**

```json
{
  "success": true,
  "message": "Suscripción cancelada exitosamente",
  "cancelledAt": "2025-12-17T18:00:00",
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

## 📊 Migración de Base de Datos

### V2025121703\_\_add_subscription_management_feature.sql

```sql
-- Insertar feature SUBSCRIPTION_MANAGEMENT
INSERT INTO features (code, name, description, is_active)
VALUES (
    'SUBSCRIPTION_MANAGEMENT',
    'Gestión de Suscripciones',
    'Permite a los super administradores gestionar las suscripciones de todas las empresas',
    TRUE
)
ON CONFLICT (code) DO NOTHING;

-- Asignar feature solo a SUPER_ADMIN (id_role = 1)
INSERT INTO role_features (id_role, id_feature)
SELECT 1, f.id_feature
FROM features f
WHERE f.code = 'SUBSCRIPTION_MANAGEMENT'
ON CONFLICT (id_role, id_feature) DO NOTHING;

-- Agregar columnas adicionales a company_subscription si no existen
ALTER TABLE company_subscription
ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(50);

-- Índice para búsquedas por fecha de cancelación
CREATE INDEX IF NOT EXISTS idx_company_subscription_cancelled
ON company_subscription (cancelled_at)
WHERE cancelled_at IS NOT NULL;
```

---

## 🏗️ Estructura de Implementación

### DTOs a Crear

1. **SubscriptionListDto** - Para listado paginado
2. **ExtendSubscriptionRequestDto** - Request para extender vigencia
3. **ExtendSubscriptionResponseDto** - Response de extensión
4. **CancelSubscriptionRequestDto** - Request para cancelar
5. **CancelSubscriptionResponseDto** - Response de cancelación

### Servicios

**ISubscriptionService** - Agregar métodos:

```java
Page<SubscriptionListDto> listAllSubscriptions(String status, String planCode, Pageable pageable);
ExtendSubscriptionResponseDto extendSubscription(Long idCompany, ExtendSubscriptionRequestDto request);
CancelSubscriptionResponseDto cancelSubscription(Long idCompany, CancelSubscriptionRequestDto request);
```

### Controlador

**SubscriptionManagementController** - Nuevo controlador con todos los endpoints

---

## 🔒 Seguridad

### Validación de Permisos

Todos los endpoints deben validar:

1. Usuario autenticado
2. Usuario tiene rol `SUPER_ADMIN`
3. Feature `SUBSCRIPTION_MANAGEMENT` está activo

### Ejemplo de Anotación

```java
@PreAuthorize("hasRole('SUPER_ADMIN')")
@FeatureEnabled("SUBSCRIPTION_MANAGEMENT")
@GetMapping("/api/subscriptions")
public ResponseEntity<Page<SubscriptionListDto>> listAll(...) {
    // Implementation
}
```

---

## ✅ Criterios de Aceptación

- [ ] Solo SUPER_ADMIN puede acceder a los endpoints
- [ ] Listado de suscripciones con paginación funciona correctamente
- [ ] Filtros por estado y plan funcionan
- [ ] Upgrade de plan actualiza correctamente la suscripción
- [ ] Extensión de vigencia calcula correctamente la nueva fecha
- [ ] Cancelación cambia el estado y registra la fecha/razón
- [ ] Validaciones de negocio implementadas
- [ ] Logs apropiados en todas las operaciones
- [ ] Documentación API actualizada
- [ ] Tests manuales completados

---

## 📝 Notas Adicionales

### Validaciones de Negocio

1. **Upgrade:**

   - El nuevo plan debe existir y estar activo
   - No se puede cambiar a un plan de menor categoría (downgrade requiere cancelación + nueva suscripción)
   - Se registra pago y referencia

2. **Extensión:**

   - Solo se pueden extender suscripciones ACTIVE o EXPIRED
   - La extensión suma meses a partir de la fecha actual de expiración
   - Se registra pago y referencia

3. **Cancelación:**
   - Solo se pueden cancelar suscripciones ACTIVE
   - Si `cancelImmediately = true`, cambia estado inmediatamente
   - Si `cancelImmediately = false`, se cancela al vencer la fecha actual
   - Se registra razón y fecha de cancelación

### Logs

Todas las operaciones deben generar logs con:

- Usuario que ejecuta la operación
- ID de empresa afectada
- Operación realizada
- Resultado (éxito/error)

---

**Fecha de Creación:** 17 de Diciembre, 2025  
**Versión:** 1.0  
**Estado:** Pendiente de Implementación
