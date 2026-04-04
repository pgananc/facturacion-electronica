# Especificación de Endpoints - Gestión de Suscripciones

## Microservicio: `ebill-billing` (Puerto 8480)

**Fecha:** 21 de Diciembre, 2025  
**Version:** 1.0  
**Autor:** Sistema de Facturación Electrónica

---

## Índice

1. [GET /api/subscriptions/available-companies](#1-get-apisubscriptionsavailable-companies)
2. [POST /api/subscriptions](#2-post-apisubscriptions)
3. [GET /api/subscriptions/plans](#3-get-apisubscriptionsplans-opcional)

---

## 1. GET /api/subscriptions/available-companies

### 📋 Descripción

Obtiene la lista de empresas (companies) que **NO tienen una suscripción activa**. Se utiliza en el diálogo "Asignar Suscripción" para mostrar solo las empresas elegibles.

### 🔐 Seguridad

- **Roles permitidos:** `SUPER_ADMIN`
- **Anotación:** `@PreAuthorize("hasRole('SUPER_ADMIN')")`
- **Token:** Bearer JWT en header `Authorization`

### 📨 Request

#### HTTP Method

```
GET /api/subscriptions/available-companies
```

#### Headers

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

#### Query Parameters

Ninguno

#### Body

Ninguno

---

### 📬 Response

#### Success Response (200 OK)

**Condición:** Empresas sin suscripción encontradas (puede ser array vacío)

```json
[
  {
    "idCompany": 1,
    "businessName": "Empresa Demo SAS",
    "ruc": "1234567890001"
  },
  {
    "idCompany": 5,
    "businessName": "Compañía de Prueba CIA. LTDA.",
    "ruc": "0987654321001"
  }
]
```

**Estructura del DTO:**

```java
public class CompanyWithoutSubscriptionDTO {
    private Long idCompany;       // ID de la empresa
    private String businessName;  // Razón social
    private String ruc;           // RUC/Identificación tributaria
}
```

#### Empty Response (200 OK)

```json
[]
```

**Nota:** Retornar array vacío si no hay empresas disponibles, **NO error 404**.

---

#### Error Responses

**401 Unauthorized**

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token inválido o expirado",
  "path": "/api/subscriptions/available-companies"
}
```

**403 Forbidden**

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "No tienes permisos para acceder a este recurso",
  "path": "/api/subscriptions/available-companies"
}
```

**500 Internal Server Error**

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Error al consultar empresas disponibles",
  "path": "/api/subscriptions/available-companies"
}
```

---

### 💾 Lógica de Negocio

#### Definición: ¿Qué es una empresa "sin suscripción activa"?

Una empresa **NO tiene suscripción activa** si cumple **cualquiera** de estas condiciones:

1. **No tiene registro** en `company_subscription` (compañías nuevas, nunca tuvieron suscripción)
2. **Tiene registro pero:**
   - `subscription_status != 'ACTIVE'` (está `EXPIRED`, `CANCELLED`, `SUSPENDED`, etc.)
   - O `expires_at < NOW()` (la suscripción ya venció)

**⚠️ IMPORTANTE:** Este endpoint debe incluir **TODAS** las compañías de `ebill-customers` que no tienen suscripción activa, incluyendo las que **nunca tuvieron suscripción** (compañías "sueltas").

#### Query SQL Correcta (Incluye compañías sin registro)

```sql
SELECT
    c.id_company AS idCompany,
    c.name AS businessName,
    c.identification AS ruc
FROM company c
LEFT JOIN company_subscription cs
    ON c.id_company = cs.id_company
    AND cs.subscription_status = 'ACTIVE'
    AND cs.expires_at > NOW()
WHERE cs.id_company_subscription IS NULL
  AND c.status = true  -- Solo compañías activas
ORDER BY c.name ASC;
```

**Explicación:**

- `LEFT JOIN` trae todas las empresas de la tabla `company`
- Condiciones en el `ON`: solo suscripciones activas y no vencidas
- `WHERE cs.id_company_subscription IS NULL`: filtra empresas sin match, lo que incluye:
  - ✅ Empresas **sin registro** en `company_subscription` (nuevas, nunca tuvieron suscripción)
  - ✅ Empresas con suscripción **EXPIRED**, **CANCELLED**, o **vencida**
- `c.status = true`: Solo incluir compañías activas

**⚠️ IMPORTANTE:** Esta query debe ejecutarse en el microservicio `ebill-billing`, pero necesita acceso a la tabla `company` del microservicio `ebill-customers`. 

**Opciones de implementación:**
1. **Cross-database query** (si están en la misma BD)
2. **Llamada HTTP** desde `ebill-billing` a `ebill-customers` para obtener lista de compañías
3. **Replicación/sincronización** de datos entre microservicios

#### Pseudo-código Java

```java
@GetMapping("/available-companies")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public ResponseEntity<List<CompanyWithoutSubscriptionDTO>> getCompaniesWithoutSubscription() {
    try {
        List<CompanyWithoutSubscriptionDTO> companies =
            companyRepository.findCompaniesWithoutActiveSubscription();
        return ResponseEntity.ok(companies);
    } catch (Exception e) {
        log.error("Error al obtener empresas sin suscripción", e);
        throw new InternalServerErrorException("Error al consultar empresas disponibles");
    }
}
```

---

### ✅ Validaciones

- ✅ Token JWT válido
- ✅ Usuario con rol `SUPER_ADMIN`
- ✅ Retornar solo empresas habilitadas (`is_active = true` en tabla `company`)

---

### 📊 Casos de Uso / Ejemplos

#### Ejemplo 1: Sistema nuevo (ninguna empresa tiene suscripción)

```json
[
  { "idCompany": 1, "businessName": "Empresa A", "ruc": "1111111111001" },
  { "idCompany": 2, "businessName": "Empresa B", "ruc": "2222222222001" },
  { "idCompany": 3, "businessName": "Empresa C", "ruc": "3333333333001" }
]
```

#### Ejemplo 2: Todas las empresas tienen suscripción activa

```json
[]
```

#### Ejemplo 3: Algunas empresas con suscripción vencida

```json
[{ "idCompany": 4, "businessName": "Empresa D (venció hace 2 meses)", "ruc": "4444444444001" }]
```

---

## 2. POST /api/subscriptions

### 📋 Descripción

Crea y asigna una **nueva suscripción** a una empresa de forma manual. Solo puede ser ejecutado por `SUPER_ADMIN`.

### 🔐 Seguridad

- **Roles permitidos:** `SUPER_ADMIN`
- **Anotación:** `@PreAuthorize("hasRole('SUPER_ADMIN')")`
- **Token:** Bearer JWT en header `Authorization`

### 📨 Request

#### HTTP Method

```
POST /api/subscriptions
```

#### Headers

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

#### Body (JSON)

```json
{
  "idCompany": 1,
  "planCode": "BASIC",
  "startDate": "2025-12-21",
  "durationMonths": 12
}
```

**Estructura del DTO:**

```java
public class AssignSubscriptionRequest {
    @NotNull(message = "El ID de la empresa es obligatorio")
    private Long idCompany;

    @NotBlank(message = "El código del plan es obligatorio")
    @Pattern(regexp = "^(FREE|BASIC|PROFESSIONAL|ENTERPRISE)$",
             message = "Plan inválido")
    private String planCode;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha debe ser hoy o futura")
    private LocalDate startDate;

    @NotNull(message = "La duración es obligatoria")
    @Min(value = 1, message = "La duración debe ser al menos 1 mes")
    @Max(value = 60, message = "La duración máxima es 60 meses")
    private Integer durationMonths;
}
```

#### Valores Permitidos

| Campo            | Tipo              | Obligatorio | Valores/Restricciones                            |
| ---------------- | ----------------- | ----------- | ------------------------------------------------ |
| `idCompany`      | Long              | ✅          | Debe existir en tabla `company`                  |
| `planCode`       | String            | ✅          | `FREE`, `BASIC`, `PROFESSIONAL`, `ENTERPRISE`    |
| `startDate`      | String (ISO 8601) | ✅          | Formato: `YYYY-MM-DD`, no puede ser fecha pasada |
| `durationMonths` | Integer           | ✅          | Min: 1, Max: 60                                  |

---

### 📬 Response

#### Success Response (201 Created)

**Condición:** Suscripción creada exitosamente

```json
{
  "idCompanySubscription": 123,
  "idCompany": 1,
  "planCode": "BASIC",
  "subscriptionStatus": "ACTIVE",
  "startDate": "2025-12-21",
  "expiresAt": "2026-12-21",
  "message": "Suscripción asignada exitosamente"
}
```

**Estructura del Response DTO (opcional, puede ser 204 No Content):**

```java
public class AssignSubscriptionResponse {
    private Long idCompanySubscription;
    private Long idCompany;
    private String planCode;
    private String subscriptionStatus;
    private LocalDate startDate;
    private LocalDate expiresAt;
    private String message;
}
```

**Alternativa (más simple):**

```
HTTP 204 No Content
```

_(Sin body, solo confirma que fue exitoso)_

---

#### Error Responses

**400 Bad Request - Validación fallida**

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Errores de validación",
  "errors": ["El ID de la empresa es obligatorio", "La fecha debe ser hoy o futura"],
  "path": "/api/subscriptions"
}
```

**400 Bad Request - Empresa ya tiene suscripción activa**

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "La empresa ya tiene una suscripción activa",
  "path": "/api/subscriptions"
}
```

**404 Not Found - Empresa no existe**

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Empresa con ID 999 no encontrada",
  "path": "/api/subscriptions"
}
```

**404 Not Found - Plan no existe**

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Plan 'INVALID_PLAN' no existe",
  "path": "/api/subscriptions"
}
```

**401 Unauthorized**

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token inválido o expirado",
  "path": "/api/subscriptions"
}
```

**403 Forbidden**

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "No tienes permisos para asignar suscripciones",
  "path": "/api/subscriptions"
}
```

**500 Internal Server Error**

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Error al crear la suscripción",
  "path": "/api/subscriptions"
}
```

---

### 💾 Lógica de Negocio

#### Validaciones Pre-Insert

1. **Validar que la empresa exista:**

   ```sql
   SELECT id_company FROM company WHERE id_company = ? AND is_active = true;
   ```

   - Si no existe → `404 Not Found`

2. **Validar que NO tenga suscripción activa:**

   ```sql
   SELECT id_company_subscription
   FROM company_subscription
   WHERE id_company = ?
     AND subscription_status = 'ACTIVE'
     AND expires_at > NOW();
   ```

   - Si existe → `400 Bad Request` ("Ya tiene suscripción activa")

3. **Validar que el plan exista:**

   ```sql
   SELECT id_subscription_plan
   FROM subscription_plan
   WHERE code = ? AND is_active = true;
   ```

   - Si no existe → `404 Not Found`

4. **Validar fecha de inicio:**
   - `startDate` no puede ser anterior a hoy
   - Si es anterior → `400 Bad Request`

#### Cálculo de Fecha de Expiración

```java
LocalDate expiresAt = startDate.plusMonths(durationMonths);
```

**Ejemplos:**

- `startDate = 2025-12-21`, `durationMonths = 12` → `expiresAt = 2026-12-21`
- `startDate = 2025-01-31`, `durationMonths = 1` → `expiresAt = 2025-02-28` (último día del mes)

#### Inserción en Base de Datos

```sql
INSERT INTO company_subscription (
    id_company,
    id_subscription_plan,
    subscription_status,
    start_date,
    expires_at,
    current_year_usage,
    created_at,
    updated_at
) VALUES (
    ?,                  -- idCompany
    (SELECT id_subscription_plan FROM subscription_plan WHERE code = ?),
    'ACTIVE',
    ?,                  -- startDate
    ?,                  -- expiresAt (calculado)
    0,                  -- Inicialmente 0 documentos usados
    NOW(),
    NOW()
);
```

#### Pseudo-código Java

```java
@PostMapping
@PreAuthorize("hasRole('SUPER_ADMIN')")
public ResponseEntity<Void> assignSubscription(
        @Valid @RequestBody AssignSubscriptionRequest request) {

    // 1. Validar que la empresa exista
    Company company = companyRepository.findById(request.getIdCompany())
        .orElseThrow(() -> new NotFoundException(
            "Empresa con ID " + request.getIdCompany() + " no encontrada"
        ));

    // 2. Validar que NO tenga suscripción activa
    boolean hasActiveSubscription = subscriptionRepository
        .existsActiveSubscriptionForCompany(request.getIdCompany());

    if (hasActiveSubscription) {
        throw new BadRequestException(
            "La empresa ya tiene una suscripción activa"
        );
    }

    // 3. Validar que el plan exista
    SubscriptionPlan plan = planRepository.findByCode(request.getPlanCode())
        .orElseThrow(() -> new NotFoundException(
            "Plan '" + request.getPlanCode() + "' no existe"
        ));

    // 4. Calcular fecha de expiración
    LocalDate expiresAt = request.getStartDate()
        .plusMonths(request.getDurationMonths());

    // 5. Crear la suscripción
    CompanySubscription subscription = new CompanySubscription();
    subscription.setIdCompany(request.getIdCompany());
    subscription.setIdSubscriptionPlan(plan.getIdSubscriptionPlan());
    subscription.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
    subscription.setStartDate(request.getStartDate());
    subscription.setExpiresAt(expiresAt);
    subscription.setCurrentYearUsage(0);
    subscription.setCreatedAt(LocalDateTime.now());
    subscription.setUpdatedAt(LocalDateTime.now());

    subscriptionRepository.save(subscription);

    log.info("Suscripción asignada: empresa={}, plan={}, expira={}",
        request.getIdCompany(), request.getPlanCode(), expiresAt);

    return ResponseEntity.status(HttpStatus.CREATED).build();
}
```

---

### ✅ Validaciones Completas

| Validación                     | Tipo          | Acción si falla    |
| ------------------------------ | ------------- | ------------------ |
| Token JWT válido               | Autenticación | `401 Unauthorized` |
| Rol `SUPER_ADMIN`              | Autorización  | `403 Forbidden`    |
| `idCompany` no nulo            | Validación    | `400 Bad Request`  |
| Empresa existe                 | Negocio       | `404 Not Found`    |
| Empresa sin suscripción activa | Negocio       | `400 Bad Request`  |
| `planCode` válido              | Validación    | `400 Bad Request`  |
| Plan existe en BD              | Negocio       | `404 Not Found`    |
| `startDate` no pasada          | Validación    | `400 Bad Request`  |
| `durationMonths` entre 1-60    | Validación    | `400 Bad Request`  |

---

### 📊 Casos de Uso / Ejemplos

#### Ejemplo 1: Asignar plan FREE por 12 meses

**Request:**

```json
{
  "idCompany": 1,
  "planCode": "FREE",
  "startDate": "2025-12-21",
  "durationMonths": 12
}
```

**Response:** `201 Created`

---

#### Ejemplo 2: Asignar plan ENTERPRISE por 24 meses (2 años)

**Request:**

```json
{
  "idCompany": 5,
  "planCode": "ENTERPRISE",
  "startDate": "2026-01-01",
  "durationMonths": 24
}
```

**Response:** `201 Created`

---

#### Ejemplo 3: Error - Empresa ya tiene suscripción activa

**Request:**

```json
{
  "idCompany": 2,
  "planCode": "BASIC",
  "startDate": "2025-12-21",
  "durationMonths": 12
}
```

**Response:** `400 Bad Request`

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "La empresa ya tiene una suscripción activa"
}
```

---

#### Ejemplo 4: Error - Fecha en el pasado

**Request:**

```json
{
  "idCompany": 1,
  "planCode": "BASIC",
  "startDate": "2024-01-01",
  "durationMonths": 12
}
```

**Response:** `400 Bad Request`

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "La fecha debe ser hoy o futura"
}
```

---

## 3. GET /api/subscriptions/plans (OPCIONAL)

### 📋 Descripción

Obtiene la lista de **planes de suscripción disponibles**. Actualmente el frontend tiene estos datos hardcoded, pero sería mejor traerlos desde la base de datos.

### 🔐 Seguridad

- **Roles permitidos:** `SUPER_ADMIN`, `ADMIN` (público para usuarios autenticados)
- **Anotación:** `@PreAuthorize("isAuthenticated()")` o sin restricción
- **Token:** Bearer JWT en header `Authorization`

### 📨 Request

#### HTTP Method

```
GET /api/subscriptions/plans
```

#### Headers

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json
```

#### Query Parameters

```
?includeInactive=false  (opcional, default: false)
```

- `includeInactive=true`: Incluye planes deshabilitados
- `includeInactive=false`: Solo planes activos

#### Body

Ninguno

---

### 📬 Response

#### Success Response (200 OK)

```json
[
  {
    "code": "FREE",
    "name": "Plan Gratuito",
    "price": 0.0,
    "documentLimit": 50,
    "features": ["Hasta 50 documentos/año", "Facturación básica", "Soporte por email"]
  },
  {
    "code": "BASIC",
    "name": "Plan Básico",
    "price": 29.99,
    "documentLimit": 500,
    "features": ["Hasta 500 documentos/año", "Notas de crédito", "Reportes operativos", "Soporte prioritario"]
  },
  {
    "code": "PROFESSIONAL",
    "name": "Plan Profesional",
    "price": 79.99,
    "documentLimit": 2000,
    "features": ["Hasta 2000 documentos/año", "Reportes avanzados", "API REST", "Webhooks", "Soporte 24/7"]
  },
  {
    "code": "ENTERPRISE",
    "name": "Plan Empresarial",
    "price": 199.99,
    "documentLimit": -1,
    "features": ["Documentos ilimitados", "Todas las características", "Soporte dedicado", "SLA garantizado", "Personalización"]
  }
]
```

**Estructura del DTO:**

```java
public class SubscriptionPlanDTO {
    private String code;              // FREE, BASIC, etc.
    private String name;              // Plan Gratuito, etc.
    private BigDecimal price;         // Precio mensual
    private Integer documentLimit;    // -1 = ilimitado
    private List<String> features;    // Características del plan
}
```

**Nota:** `documentLimit = -1` significa **ilimitado**.

---

#### Error Responses

**401 Unauthorized** (si se requiere autenticación)

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token inválido o expirado",
  "path": "/api/subscriptions/plans"
}
```

**500 Internal Server Error**

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Error al consultar planes de suscripción",
  "path": "/api/subscriptions/plans"
}
```

---

### 💾 Lógica de Negocio

#### Query SQL Sugerido

```sql
SELECT
    code,
    name,
    price,
    document_limit_yearly AS documentLimit,
    features  -- Almacenado como JSON o TEXT separado por comas
FROM subscription_plan
WHERE is_active = true
ORDER BY price ASC;
```

**Nota sobre `features`:**

- Opción 1: Columna JSON en PostgreSQL
- Opción 2: Tabla separada `subscription_plan_feature` (relación 1:N)
- Opción 3: String separado por comas (menos recomendado)

#### Pseudo-código Java

```java
@GetMapping("/plans")
public ResponseEntity<List<SubscriptionPlanDTO>> getAvailablePlans(
        @RequestParam(defaultValue = "false") boolean includeInactive) {

    try {
        List<SubscriptionPlanDTO> plans;

        if (includeInactive) {
            plans = planRepository.findAll();
        } else {
            plans = planRepository.findByIsActiveTrue();
        }

        return ResponseEntity.ok(plans);
    } catch (Exception e) {
        log.error("Error al consultar planes", e);
        throw new InternalServerErrorException(
            "Error al consultar planes de suscripción"
        );
    }
}
```

---

### ✅ Validaciones

- ✅ Token JWT válido (opcional, depende del nivel de seguridad deseado)
- ✅ Retornar solo planes activos por defecto

---

### 📊 Casos de Uso / Ejemplos

#### Ejemplo 1: Consulta estándar (solo planes activos)

**Request:**

```
GET /api/subscriptions/plans
```

**Response:** `200 OK` - 4 planes activos

---

#### Ejemplo 2: Incluir planes deshabilitados

**Request:**

```
GET /api/subscriptions/plans?includeInactive=true
```

**Response:** `200 OK` - Todos los planes (incluyendo inactivos)

---

## 📝 Notas de Implementación

### Orden de Prioridad

1. **CRÍTICO:** `GET /api/subscriptions/available-companies`
2. **CRÍTICO:** `POST /api/subscriptions`
3. **OPCIONAL:** `GET /api/subscriptions/plans`

### Consideraciones de Base de Datos

#### Tablas Involucradas

- `company`
- `company_subscription`
- `subscription_plan`

#### Índices Recomendados

```sql
CREATE INDEX idx_company_subscription_active
ON company_subscription(id_company, subscription_status, expires_at);

CREATE INDEX idx_subscription_plan_code
ON subscription_plan(code);
```

### Logs Sugeridos

```java
// En cada endpoint
log.info("Usuario {} ejecutando operación: {}",
    username, operationName);

log.info("Suscripción asignada: empresa={}, plan={}, expira={}",
    idCompany, planCode, expiresAt);

log.error("Error al asignar suscripción a empresa {}: {}",
    idCompany, e.getMessage());
```

---

## ✅ Checklist de Implementación

### Backend (ebill-billing)

- [ ] Crear DTOs: `CompanyWithoutSubscriptionDTO`, `AssignSubscriptionRequest`
- [ ] Crear endpoint `GET /api/subscriptions/available-companies`
- [ ] Crear endpoint `POST /api/subscriptions`
- [ ] Implementar validaciones de negocio
- [ ] Agregar tests unitarios
- [ ] Agregar tests de integración
- [ ] Documentar en Swagger/OpenAPI
- [ ] Probar con Postman/cURL
- [ ] Verificar logs en servidor

### Frontend (ya implementado)

- [x] Servicio `SubscriptionAdminService`
- [x] Componente `SubscriptionListComponent`
- [x] Diálogo `AssignSubscriptionDialogComponent`
- [x] Guards de permisos
- [x] Toasts de feedback

### Testing E2E

- [ ] Probar asignación de suscripción exitosa
- [ ] Probar error: empresa con suscripción activa
- [ ] Probar error: fecha en el pasado
- [ ] Probar error: plan inválido
- [ ] Probar listado de empresas sin suscripción
- [ ] Probar permisos (solo super_admin)

---

## 🔗 Referencias

- **Microservicio:** `ebill-billing` - Puerto 8480
- **Controller:** `SubscriptionManagementController`
- **Frontend Service:** `subscription-admin.service.ts`
- **Documentación adicional:** `SUBSCRIPTION-MANAGEMENT-TOASTS.md`

---

## 📡 Ejemplos de cURL y Responses Reales

### 1️⃣ GET /api/subscriptions/available-companies

**cURL:**

```bash
curl --location 'http://localhost:8480/api/subscriptions/available-companies' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...'
```

**Response (200 OK):**

```json
[
  {
    "idCompany": 3,
    "businessName": "Empresa Sin Suscripción SAS",
    "ruc": "1234567890001"
  },
  {
    "idCompany": 7,
    "businessName": "Nueva Compañía CIA. LTDA.",
    "ruc": "0987654321001"
  }
]
```

**Si no hay empresas disponibles:**

```json
[]
```

---

### 2️⃣ POST /api/subscriptions (Asignar Suscripción)

**cURL:**

```bash
curl --location 'http://localhost:8480/api/subscriptions' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...' \
--header 'Content-Type: application/json' \
--data '{
  "idCompany": 3,
  "planCode": "BASIC",
  "startDate": "2025-12-21",
  "durationMonths": 12
}'
```

**Response (201 Created) - Opción 1: Sin body**

```
HTTP 201 Created
(Sin body)
```

**Response (201 Created) - Opción 2: Con body**

```json
{
  "idCompanySubscription": 45,
  "idCompany": 3,
  "planCode": "BASIC",
  "subscriptionStatus": "ACTIVE",
  "expiresAt": "2026-12-21T00:00:00"
}
```

**Errores comunes:**

400 - Empresa ya tiene suscripción activa:

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "La empresa ya tiene una suscripción activa",
  "path": "/api/subscriptions"
}
```

400 - Plan no existe:

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Plan 'INVALID' no existe",
  "path": "/api/subscriptions"
}
```

400 - Fecha en el pasado:

```json
{
  "timestamp": "2025-12-21T15:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "La fecha debe ser hoy o futura",
  "path": "/api/subscriptions"
}
```

---

### 3️⃣ GET /api/subscriptions/plans

**cURL básico:**

```bash
curl --location 'http://localhost:8480/api/subscriptions/plans' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...'
```

**cURL con planes inactivos:**

```bash
curl --location 'http://localhost:8480/api/subscriptions/plans?includeInactive=true' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...'
```

**Response (200 OK):**

```json
[
  {
    "code": "FREE",
    "name": "Plan Gratuito",
    "priceMonthly": 0.0,
    "documentLimitYearly": 50,
    "features": ["Hasta 50 documentos/año", "Facturación electrónica", "Soporte por email"]
  },
  {
    "code": "BASIC",
    "name": "Plan Básico",
    "priceMonthly": 29.99,
    "documentLimitYearly": 500,
    "features": ["Hasta 500 documentos/año", "Facturación electrónica", "Notas de crédito", "Múltiples usuarios", "Soporte prioritario"]
  },
  {
    "code": "PROFESSIONAL",
    "name": "Plan Profesional",
    "priceMonthly": 79.99,
    "documentLimitYearly": 2000,
    "features": ["Hasta 2000 documentos/año", "Facturación electrónica", "Notas de crédito", "Notas de débito", "Retenciones", "Múltiples usuarios", "Reportes avanzados", "Acceso API REST", "Soporte prioritario"]
  },
  {
    "code": "ENTERPRISE",
    "name": "Plan Empresarial",
    "priceMonthly": 199.99,
    "documentLimitYearly": -1,
    "features": ["Documentos ilimitados", "Facturación electrónica", "Notas de crédito", "Notas de débito", "Retenciones", "Guías de remisión", "Múltiples usuarios", "Reportes avanzados", "Acceso API REST", "Soporte prioritario", "Personalización de marca", "Respaldos automáticos"]
  }
]
```

---

### 4️⃣ GET /api/subscriptions (Listar todas - SUPER_ADMIN)

**cURL básico:**

```bash
curl --location 'http://localhost:8480/api/subscriptions' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...'
```

**cURL con filtros:**

```bash
curl --location 'http://localhost:8480/api/subscriptions?status=ACTIVE&planCode=BASIC&page=0&size=10' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...'
```

**Response (200 OK):**

```json
{
  "content": [
    {
      "idSubscription": 1,
      "idCompany": 1,
      "plan": {
        "idPlan": 2,
        "code": "BASIC",
        "name": "Plan Básico",
        "documentLimitYearly": 500,
        "priceAnnual": 299.88,
        "priceMonthly": 29.99
      },
      "subscriptionStatus": "ACTIVE",
      "startDate": "2025-01-01T00:00:00",
      "expiresAt": "2026-01-01T00:00:00",
      "currentYearUsage": 123,
      "percentUsed": 24.6,
      "documentsRemaining": 377,
      "isExpired": false,
      "isLimitReached": false,
      "cancelledAt": null,
      "cancelReason": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true
}
```

---

### 5️⃣ GET /api/subscriptions/company/{idCompany}

**cURL:**

```bash
curl --location 'http://localhost:8480/api/subscriptions/company/1' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...'
```

**Response (200 OK):**

```json
{
  "idSubscription": 1,
  "idCompany": 1,
  "plan": {
    "idPlan": 2,
    "code": "BASIC",
    "name": "Plan Básico",
    "description": "Plan ideal para pequeñas empresas",
    "priceAnnual": 299.88,
    "priceMonthly": 29.99,
    "documentLimitYearly": 500,
    "userLimit": 5,
    "establishmentLimit": 2,
    "issuancePointLimit": 5,
    "hasApiAccess": false,
    "hasMultiUser": true,
    "hasAdvancedReports": false,
    "hasElectronicInvoice": true,
    "hasCreditNote": true,
    "hasDebitNote": false,
    "hasRetention": false,
    "hasGuide": false,
    "hasPrioritySupport": true,
    "hasCustomBranding": false,
    "hasAutomatedBackups": false,
    "isActive": true,
    "displayOrder": 2
  },
  "subscriptionStatus": "ACTIVE",
  "startDate": "2025-01-01T00:00:00",
  "expiresAt": "2026-01-01T00:00:00",
  "currentYearUsage": 123,
  "lastResetDate": "2025-01-01T00:00:00",
  "percentUsed": 24.6,
  "documentsRemaining": 377,
  "isExpired": false,
  "isLimitReached": false,
  "paymentMethod": "TARJETA",
  "lastPaymentDate": "2025-01-01T10:30:00",
  "lastPaymentAmount": 299.88,
  "paymentReference": "REF-2025-001",
  "notes": "Suscripción pagada anualmente"
}
```

---

### 6️⃣ POST /api/subscriptions/company/{idCompany}/upgrade (Cambiar Plan)

**cURL:**

```bash
curl --location 'http://localhost:8480/api/subscriptions/company/1/upgrade' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...' \
--header 'Content-Type: application/json' \
--data '{
  "newPlanCode": "PROFESSIONAL",
  "paymentMethod": "TRANSFERENCIA",
  "paymentAmount": 959.88,
  "paymentReference": "TRX-2025-456",
  "notes": "Upgrade a plan profesional por crecimiento"
}'
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Plan actualizado exitosamente de BASIC a PROFESSIONAL",
  "previousPlan": {
    "code": "BASIC",
    "name": "Plan Básico"
  },
  "newPlan": {
    "code": "PROFESSIONAL",
    "name": "Plan Profesional"
  },
  "newExpiryDate": "2026-12-21T00:00:00",
  "subscription": {
    "idSubscription": 1,
    "idCompany": 1,
    "subscriptionStatus": "ACTIVE",
    "expiresAt": "2026-12-21T00:00:00"
  }
}
```

---

### 7️⃣ POST /api/subscriptions/company/{idCompany}/extend (Extender Vigencia)

**cURL:**

```bash
curl --location 'http://localhost:8480/api/subscriptions/company/1/extend' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...' \
--header 'Content-Type: application/json' \
--data '{
  "extensionMonths": 6,
  "paymentMethod": "TRANSFERENCIA",
  "paymentAmount": 179.94,
  "paymentReference": "EXT-2025-789",
  "notes": "Extensión semestral"
}'
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Suscripción extendida exitosamente",
  "previousExpiryDate": "2026-01-01T00:00:00",
  "newExpiryDate": "2026-07-01T00:00:00",
  "extensionMonths": 6,
  "subscription": {
    "idSubscription": 1,
    "idCompany": 1,
    "subscriptionStatus": "ACTIVE",
    "expiresAt": "2026-07-01T00:00:00"
  }
}
```

---

### 8️⃣ DELETE /api/subscriptions/company/{idCompany}/cancel (Cancelar)

**cURL:**

```bash
curl --location --request DELETE 'http://localhost:8480/api/subscriptions/company/1/cancel' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...' \
--header 'Content-Type: application/json' \
--data '{
  "reason": "PAYMENT_FAILURE",
  "cancelImmediately": true,
  "notes": "Cliente no realizó el pago"
}'
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Suscripción cancelada exitosamente",
  "cancelledAt": "2025-12-21T15:45:30",
  "subscription": {
    "idSubscription": 1,
    "idCompany": 1,
    "subscriptionStatus": "CANCELLED",
    "cancelledAt": "2025-12-21T15:45:30",
    "cancelReason": "PAYMENT_FAILURE"
  }
}
```

---

## 📋 Resumen de URLs

### ✅ TODOS IMPLEMENTADOS Y FUNCIONANDO:

**Endpoints de gestión (super_admin):**

```
GET    http://localhost:8480/api/subscriptions/available-companies  ← Lista empresas sin suscripción
POST   http://localhost:8480/api/subscriptions                      ← Asignar nueva suscripción
GET    http://localhost:8480/api/subscriptions/plans                ← Planes disponibles
GET    http://localhost:8480/api/subscriptions                      ← Listar todas las suscripciones
GET    http://localhost:8480/api/subscriptions/company/{id}         ← Ver suscripción específica
POST   http://localhost:8480/api/subscriptions/company/{id}/upgrade ← Cambiar plan
POST   http://localhost:8480/api/subscriptions/company/{id}/extend  ← Extender vigencia
DELETE http://localhost:8480/api/subscriptions/company/{id}/cancel  ← Cancelar suscripción
```

**Nota:** Todos requieren JWT token de `super_admin` en el header:  
`Authorization: Bearer <tu-token-aqui>`

---

## ✅ Estado de Implementación

| Endpoint                 | Backend         | Frontend     | Estado             |
| ------------------------ | --------------- | ------------ | ------------------ |
| GET /available-companies | ✅ Implementado | ✅ Listo     | 🟢 Funcional       |
| POST /subscriptions      | ✅ Implementado | ✅ Listo     | 🟢 Funcional       |
| GET /plans               | ✅ Implementado | ⚠️ Hardcoded | 🟡 Opcional migrar |
| GET /subscriptions       | ✅ Implementado | ✅ Listo     | 🟢 Funcional       |
| GET /company/{id}        | ✅ Implementado | ✅ Listo     | 🟢 Funcional       |
| POST /upgrade            | ✅ Implementado | ✅ Listo     | 🟢 Funcional       |
| POST /extend             | ✅ Implementado | ✅ Listo     | 🟢 Funcional       |
| DELETE /cancel           | ✅ Implementado | ✅ Listo     | 🟢 Funcional       |

---

**Última actualización:** 21/12/2025  
**Estado:** ✅ Todos los endpoints implementados y documentados
