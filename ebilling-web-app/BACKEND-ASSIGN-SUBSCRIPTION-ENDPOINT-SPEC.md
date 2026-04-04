# 📋 Especificación Técnica - Endpoint Asignar Suscripción

**Fecha:** 21 de Diciembre, 2025  
**Versión:** 1.0  
**Prioridad:** 🔴 ALTA  
**Estado:** ⏳ Pendiente de Implementación

---

## 🎯 Objetivo

Implementar el endpoint REST para asignar una nueva suscripción a una empresa que actualmente no tiene suscripción activa.

---

## 📡 Endpoint

### **POST** `/api/subscriptions`

**Descripción:** Crea una nueva suscripción para una empresa específica.

**Autenticación:** Requerida (JWT Token)  
**Autorización:** Solo usuarios con rol `SUPER_ADMIN`

---

## 📥 Request Body

### Estructura JSON

```json
{
  "idCompany": 123,
  "planCode": "BASIC",
  "startDate": "2025-12-21",
  "durationMonths": 12
}
```

### Campos Requeridos

| Campo            | Tipo                | Descripción                                          | Validaciones                                                                                |
| ---------------- | ------------------- | ---------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| `idCompany`      | `integer`           | ID de la empresa a la que se asignará la suscripción | - Debe existir en la BD<br>- No debe tener suscripción activa                               |
| `planCode`       | `string`            | Código del plan de suscripción                       | - Debe existir en la BD<br>- Valores válidos: `FREE`, `BASIC`, `PROFESSIONAL`, `ENTERPRISE` |
| `startDate`      | `string` (ISO 8601) | Fecha de inicio de la suscripción                    | - Formato: `YYYY-MM-DD`<br>- No puede ser fecha pasada<br>- Requerido                       |
| `durationMonths` | `integer`           | Duración de la suscripción en meses                  | - Mínimo: 1<br>- Máximo: 60<br>- Requerido                                                  |

### Ejemplo de Request

```http
POST /api/subscriptions HTTP/1.1
Host: api.ebilling.com
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "idCompany": 45,
  "planCode": "PROFESSIONAL",
  "startDate": "2025-12-21",
  "durationMonths": 24
}
```

---

## 📤 Response

### ✅ Éxito (201 Created)

**Código HTTP:** `201 Created`

**Body:** Vacío (sin contenido)

**Headers:**

```
Location: /api/subscriptions/company/45
```

### Ejemplo de Response Exitosa

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/subscriptions/company/45

{}
```

---

## ❌ Errores

### 400 Bad Request - Datos Inválidos

**Código HTTP:** `400 Bad Request`

**Body:**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Datos inválidos. Verifica la información",
  "timestamp": "2025-12-21T10:30:00Z",
  "path": "/api/subscriptions",
  "errors": [
    {
      "field": "durationMonths",
      "message": "La duración debe estar entre 1 y 60 meses"
    }
  ]
}
```

**Casos de uso:**

- Campos faltantes o nulos
- `durationMonths` fuera del rango 1-60
- `startDate` en formato inválido
- `planCode` con valor inválido

---

### 401 Unauthorized - No Autenticado

**Código HTTP:** `401 Unauthorized`

**Body:**

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Token de autenticación inválido o expirado",
  "timestamp": "2025-12-21T10:30:00Z",
  "path": "/api/subscriptions"
}
```

---

### 403 Forbidden - Sin Permisos

**Código HTTP:** `403 Forbidden`

**Body:**

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "No tienes permisos para realizar esta acción. Se requiere rol SUPER_ADMIN",
  "timestamp": "2025-12-21T10:30:00Z",
  "path": "/api/subscriptions"
}
```

---

### 404 Not Found - Recurso No Encontrado

**Código HTTP:** `404 Not Found`

**Body:**

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Empresa con ID 123 no encontrada",
  "timestamp": "2025-12-21T10:30:00Z",
  "path": "/api/subscriptions"
}
```

**Casos de uso:**

- `idCompany` no existe en la BD
- `planCode` no existe en la BD

---

### 409 Conflict - Suscripción Activa Existente

**Código HTTP:** `409 Conflict`

**Body:**

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "La empresa ya tiene una suscripción activa",
  "timestamp": "2025-12-21T10:30:00Z",
  "path": "/api/subscriptions",
  "details": {
    "idCompany": 45,
    "existingSubscriptionId": 789,
    "expiresAt": "2026-12-20"
  }
}
```

**Caso de uso:**

- La empresa ya tiene una suscripción con estado `ACTIVE` o `TRIAL`

---

### 500 Internal Server Error

**Código HTTP:** `500 Internal Server Error`

**Body:**

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Error interno del servidor. Contacta al administrador",
  "timestamp": "2025-12-21T10:30:00Z",
  "path": "/api/subscriptions"
}
```

---

## 🔍 Validaciones Backend Requeridas

### 1. Validación de Empresa

```java
// Pseudocódigo
Company company = companyRepository.findById(request.getIdCompany())
    .orElseThrow(() -> new NotFoundException(
        "Empresa con ID " + request.getIdCompany() + " no encontrada"
    ));
```

### 2. Validación de Suscripción Activa

```java
// Pseudocódigo
boolean hasActiveSubscription = subscriptionRepository
    .existsActiveSubscriptionForCompany(request.getIdCompany());

if (hasActiveSubscription) {
    throw new ConflictException(
        "La empresa ya tiene una suscripción activa"
    );
}
```

**Nota:** Una suscripción se considera "activa" si su estado es:

- `ACTIVE`
- `TRIAL`

**NO se considera activa si es:**

- `EXPIRED`
- `CANCELLED`

### 3. Validación de Plan

```java
// Pseudocódigo
SubscriptionPlan plan = planRepository.findByCode(request.getPlanCode())
    .orElseThrow(() -> new NotFoundException(
        "Plan '" + request.getPlanCode() + "' no existe"
    ));
```

### 4. Validación de Fecha de Inicio

```java
// Pseudocódigo
LocalDate startDate = request.getStartDate();
LocalDate today = LocalDate.now();

if (startDate.isBefore(today)) {
    throw new BadRequestException(
        "La fecha de inicio no puede ser anterior a hoy"
    );
}
```

### 5. Validación de Duración

```java
// Pseudocódigo
if (request.getDurationMonths() < 1 || request.getDurationMonths() > 60) {
    throw new BadRequestException(
        "La duración debe estar entre 1 y 60 meses"
    );
}
```

---

## 🗄️ Lógica de Negocio

### Cálculo de Fecha de Expiración

```java
// Pseudocódigo
LocalDate expiresAt = request.getStartDate()
    .plusMonths(request.getDurationMonths())
    .minusDays(1); // Un día antes del inicio del siguiente mes
```

**Ejemplo:**

- `startDate`: `2025-12-21`
- `durationMonths`: `12`
- `expiresAt`: `2026-12-20` (21 dic 2025 + 12 meses - 1 día)

### Creación de Suscripción

```java
// Pseudocódigo
CompanySubscription subscription = new CompanySubscription();
subscription.setIdCompany(request.getIdCompany());
subscription.setIdSubscriptionPlan(plan.getIdSubscriptionPlan());
subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
subscription.setStartDate(request.getStartDate());
subscription.setExpiresAt(expiresAt);
subscription.setCurrentYearUsage(0); // Iniciar en 0
subscription.setCreatedAt(LocalDateTime.now());
subscription.setUpdatedAt(LocalDateTime.now());

subscriptionRepository.save(subscription);
```

---

## 🔐 Seguridad

### Autenticación

- **Método:** JWT Bearer Token
- **Header:** `Authorization: Bearer <token>`
- **Validación:** Token debe ser válido y no expirado

### Autorización

- **Rol requerido:** `SUPER_ADMIN`
- **Validación:** Usar `@PreAuthorize("hasRole('SUPER_ADMIN')")` en Spring Security

### Validación de Entrada

- **Sanitización:** Validar y sanitizar todos los campos de entrada
- **SQL Injection:** Usar Prepared Statements / JPA
- **XSS:** Validar formato de fechas y códigos de plan

---

## 📊 Modelo de Datos Esperado

### Tabla: `company_subscription`

| Campo                  | Tipo                | Descripción                        | Constraints                                                      |
| ---------------------- | ------------------- | ---------------------------------- | ---------------------------------------------------------------- |
| `id_subscription`      | `BIGINT` (PK, Auto) | ID único de la suscripción         | PRIMARY KEY, AUTO_INCREMENT                                      |
| `id_company`           | `INT` (FK)          | ID de la empresa                   | NOT NULL, FOREIGN KEY → `company.id_company`                     |
| `id_subscription_plan` | `INT` (FK)          | ID del plan de suscripción         | NOT NULL, FOREIGN KEY → `subscription_plan.id_subscription_plan` |
| `subscription_status`  | `VARCHAR(20)`       | Estado de la suscripción           | NOT NULL, CHECK (`ACTIVE`, `TRIAL`, `EXPIRED`, `CANCELLED`)      |
| `start_date`           | `DATE`              | Fecha de inicio                    | NOT NULL                                                         |
| `expires_at`           | `DATE`              | Fecha de expiración                | NOT NULL                                                         |
| `current_year_usage`   | `INT`               | Documentos usados en el año actual | NOT NULL, DEFAULT 0                                              |
| `created_at`           | `TIMESTAMP`         | Fecha de creación                  | NOT NULL, DEFAULT CURRENT_TIMESTAMP                              |
| `updated_at`           | `TIMESTAMP`         | Fecha de última actualización      | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  |

### Tabla: `subscription_plan`

| Campo                   | Tipo            | Descripción                                   |
| ----------------------- | --------------- | --------------------------------------------- |
| `id_subscription_plan`  | `INT` (PK)      | ID único del plan                             |
| `code`                  | `VARCHAR(20)`   | Código del plan (ej: `BASIC`, `PROFESSIONAL`) |
| `name`                  | `VARCHAR(100)`  | Nombre del plan                               |
| `monthly_price`         | `DECIMAL(10,2)` | Precio mensual                                |
| `document_limit_yearly` | `INT`           | Límite de documentos anuales (-1 = ilimitado) |

---

## 🧪 Casos de Prueba Sugeridos

### Caso 1: Asignación Exitosa

- **Input:** Empresa sin suscripción, plan válido, fecha futura
- **Expected:** `201 Created`, suscripción creada con estado `ACTIVE`

### Caso 2: Empresa No Encontrada

- **Input:** `idCompany: 99999` (no existe)
- **Expected:** `404 Not Found`

### Caso 3: Plan No Encontrado

- **Input:** `planCode: "INVALID_PLAN"`
- **Expected:** `404 Not Found`

### Caso 4: Empresa con Suscripción Activa

- **Input:** Empresa con suscripción `ACTIVE`
- **Expected:** `409 Conflict`

### Caso 5: Fecha Pasada

- **Input:** `startDate: "2020-01-01"`
- **Expected:** `400 Bad Request`

### Caso 6: Duración Inválida

- **Input:** `durationMonths: 0` o `durationMonths: 61`
- **Expected:** `400 Bad Request`

### Caso 7: Sin Autenticación

- **Input:** Request sin header `Authorization`
- **Expected:** `401 Unauthorized`

### Caso 8: Sin Permisos

- **Input:** Usuario con rol diferente a `SUPER_ADMIN`
- **Expected:** `403 Forbidden`

---

## 🔗 Endpoints Relacionados

### GET `/api/subscriptions/available-companies`

**Estado:** ✅ Ya implementado  
**Uso:** Obtener lista de empresas sin suscripción activa

### GET `/api/subscriptions/plans`

**Estado:** ⏳ Pendiente (opcional)  
**Uso:** Obtener planes disponibles desde backend (actualmente hardcoded en frontend)

---

## 📝 Notas de Implementación

### Consideraciones Adicionales

1. **Transaccionalidad:** La creación de la suscripción debe ser atómica (usar `@Transactional`)

2. **Logging:** Registrar todas las asignaciones exitosas y errores:

   ```java
   log.info("Suscripción asignada: empresa={}, plan={}, expira={}",
       request.getIdCompany(), request.getPlanCode(), expiresAt);
   ```

3. **Auditoría:** Considerar crear registro en tabla de auditoría

4. **Notificaciones:** (Opcional) Enviar email a la empresa cuando se asigne la suscripción

5. **Validación de Negocio:** Verificar que el plan existe y está activo antes de asignar

---

## ✅ Checklist de Implementación

- [ ] Crear DTO `AssignSubscriptionRequest` con validaciones
- [ ] Implementar endpoint `POST /api/subscriptions`
- [ ] Agregar validación de empresa existente
- [ ] Agregar validación de suscripción activa
- [ ] Agregar validación de plan existente
- [ ] Agregar validación de fecha de inicio (no pasada)
- [ ] Agregar validación de duración (1-60 meses)
- [ ] Implementar cálculo de fecha de expiración
- [ ] Crear registro en tabla `company_subscription`
- [ ] Configurar autorización `SUPER_ADMIN`
- [ ] Manejar errores 400, 401, 403, 404, 409, 500
- [ ] Agregar logging de operaciones
- [ ] Escribir tests unitarios
- [ ] Escribir tests de integración
- [ ] Documentar en Swagger/OpenAPI
- [ ] Validar con frontend (descomentar código en `assign-subscription-dialog.component.ts`)

---

## 📞 Contacto Frontend

**Componente:** `AssignSubscriptionDialogComponent`  
**Archivo:** `src/app/pages/admin/subscriptions/dialogs/assign-subscription-dialog/assign-subscription-dialog.component.ts`  
**Método:** `onAssign()` (líneas 270-349)

**Código actualmente comentado:**

```typescript
this.subscriptionService.assignSubscription(formData).subscribe({
  next: (response) => {
    /* ... */
  },
  error: (error) => {
    /* ... */
  },
});
```

**Servicio:** `SubscriptionAdminService`  
**Método requerido:** `assignSubscription(request: AssignSubscriptionRequest): Observable<void>`

---

**Autor:** Frontend Team  
**Fecha de Creación:** 21/12/2025  
**Última Actualización:** 21/12/2025
