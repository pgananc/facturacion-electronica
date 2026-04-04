# 🔧 Fix: Permission Validation 400 Error

## 📋 Resumen del Problema

El frontend está recibiendo **errores HTTP 400 (Bad Request)** al intentar validar permisos con el endpoint `/api/permission/validate-feature`. Esto causa:

- ❌ **12+ llamadas fallidas** con el mismo error
- 🔔 **Toast spam** con el mensaje "No tienes permisos para acceder a esta sección"
- 🚫 **Acceso denegado** a la página `/welcome` que requiere el permiso `dashboard.view`

---

## 🔍 Análisis del Error

### Endpoint Afectado

```
POST http://localhost:8280/api/permission/validate-feature
```

### Request Payload Enviado

```json
{
  "featureCode": "dashboard.view",
  "idCompany": 123,
  "idUser": 456
}
```

### Response Actual

```
HTTP 400 Bad Request
```

### Response Esperada

```json
{
  "allowed": true,
  "message": "Acceso permitido",
  "reason": null
}
```

---

## 🛠️ Soluciones Implementadas (Frontend)

### ✅ 1. Enhanced Debugging

Se agregó logging detallado en `PermissionService` para diagnosticar el problema:

```typescript
// Logs agregados:
- Token validation (idCompany presente)
- Request payload completo
- Response details (status, error, url)
- Mensajes específicos por tipo de error (400, 404, etc.)
```

### ✅ 2. Toast Deduplication

Se implementó un sistema de caché en `PermissionGuard` para evitar toast spam:

```typescript
// Cache de errores con TTL de 1 minuto
private readonly shownErrors = new Set<string>();
private readonly ERROR_CACHE_TTL = 60000;
```

**Comportamiento:**

- El mismo error solo se muestra **una vez cada 60 segundos**
- Se usa una clave única: `${permission}-${reason}`

### ✅ 3. Better Error Handling

Se mejoró el manejo de errores para proporcionar mensajes más específicos:

- `400`: "Solicitud inválida. Verifique la configuración del sistema."
- `404`: "Servicio de permisos no disponible."
- Otros: "Error al verificar permisos"

---

## 🎯 Pasos para Resolver (Backend)

### 1️⃣ Verificar que el Endpoint Existe

**Microservicio:** `auth-service` (puerto 8280)

**Controller esperado:**

```java
@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    @PostMapping("/validate-feature")
    public ResponseEntity<FeatureValidationResponse> validateFeature(
        @RequestBody FeatureValidationRequest request
    ) {
        // Implementación
    }
}
```

### 2️⃣ Validar el DTO de Request

```java
public class FeatureValidationRequest {
    @NotBlank
    private String featureCode;

    @NotNull
    private Long idCompany;

    private Long idUser; // Opcional

    // Getters, setters, constructors
}
```

### 3️⃣ Validar el DTO de Response

```java
public class FeatureValidationResponse {
    private boolean allowed;
    private String reason; // "NO_ROLE", "NO_SUBSCRIPTION", "LIMIT_EXCEEDED", "EXPIRED"
    private String message;
    private String upgradeUrl; // Opcional

    // Getters, setters, constructors
}
```

### 4️⃣ Implementar la Lógica de Validación

```java
@Service
public class PermissionService {

    public FeatureValidationResponse validateFeature(
        String featureCode,
        Long idCompany,
        Long idUser
    ) {
        // 1. Obtener roles del usuario
        List<String> userRoles = getUserRoles(idUser);

        // 2. Obtener feature desde DB
        Feature feature = featureRepository.findByCode(featureCode)
            .orElseThrow(() -> new FeatureNotFoundException(featureCode));

        // 3. Verificar si el rol del usuario está en allowed_roles de la feature
        boolean hasRole = userRoles.stream()
            .anyMatch(role -> feature.getAllowedRoles().contains(role));

        if (!hasRole) {
            return FeatureValidationResponse.denied(
                "NO_ROLE",
                "Tu rol no tiene permisos para esta funcionalidad"
            );
        }

        // 4. Obtener suscripción de la compañía
        CompanySubscription subscription =
            subscriptionRepository.findByCompanyId(idCompany)
                .orElse(null);

        if (subscription == null || !subscription.isActive()) {
            return FeatureValidationResponse.denied(
                "NO_SUBSCRIPTION",
                "Requiere una suscripción activa"
            );
        }

        // 5. Verificar si el plan incluye la feature
        String planCode = subscription.getPlan().getCode();
        boolean planHasFeature = feature.getRequiredPlans().contains(planCode);

        if (!planHasFeature) {
            return FeatureValidationResponse.denied(
                "NO_SUBSCRIPTION",
                "Esta funcionalidad no está incluida en tu plan actual"
            );
        }

        // ✅ Todo OK
        return FeatureValidationResponse.allowed("Acceso permitido");
    }
}
```

### 5️⃣ Verificar la Tabla `features` en la Base de Datos

```sql
-- Verificar que exista la feature dashboard.view
SELECT * FROM features WHERE feature_code = 'dashboard.view';

-- Resultado esperado:
-- feature_code: 'dashboard.view'
-- feature_name: 'Ver Dashboard'
-- description: 'Acceso al Welcome Dashboard con KPIs'
-- module: 'DASHBOARD'
-- allowed_roles: {3} -- Array con ID del rol 'admin'
-- required_plans: {'BASIC','PRO','BUSINESS','ENTERPRISE'}
-- is_active: true
```

Si no existe, ejecutar:

```sql
INSERT INTO features (
    feature_code,
    feature_name,
    description,
    module,
    allowed_roles,
    required_plans,
    is_active
) VALUES (
    'dashboard.view',
    'Ver Dashboard',
    'Acceso al Welcome Dashboard con KPIs y gráficos',
    'DASHBOARD',
    ARRAY[3]::INTEGER[],  -- Rol admin (ajustar según tu DB)
    ARRAY['BASIC','PRO','BUSINESS','ENTERPRISE']::VARCHAR[],
    true
);
```

---

## 🧪 Cómo Probar

### Opción 1: cURL directo al backend

```bash
# Obtener el token del localStorage del navegador
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# Ejecutar request
curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "featureCode": "dashboard.view",
    "idCompany": 1,
    "idUser": 1
  }'
```

**Response esperada (éxito):**

```json
{
  "allowed": true,
  "message": "Acceso permitido"
}
```

**Response esperada (sin permiso):**

```json
{
  "allowed": false,
  "reason": "NO_SUBSCRIPTION",
  "message": "Esta funcionalidad requiere un plan superior"
}
```

### Opción 2: Usar el Frontend con DevTools

1. Abrir DevTools → Console
2. Navegar a `/welcome`
3. Buscar logs que empiecen con `[PermissionService]`
4. Verificar:
   - ✅ `request` tiene `idCompany` válido
   - ✅ `endpoint` apunta a `http://localhost:8280/api/permission/validate-feature`
   - ✅ Response status es `200` (no `400`)

---

## 📊 Features a Implementar

Según la arquitectura actual, estos son los permisos que el backend debe soportar:

| Feature Code           | Plan Mínimo | Ruta Frontend                |
| ---------------------- | ----------- | ---------------------------- |
| `dashboard.view`       | BASIC       | `/welcome`                   |
| `reports.advanced`     | PRO         | `/reports/sales-dashboard`   |
| `reports.advanced`     | PRO         | `/reports/branch-comparison` |
| `billing.draft.manage` | BASIC       | `/invoices/drafts`           |

---

## ✅ Checklist de Implementación Backend

- [ ] Crear `PermissionController` en `auth-service`
- [ ] Implementar endpoint `POST /api/permission/validate-feature`
- [ ] Crear DTOs: `FeatureValidationRequest` y `FeatureValidationResponse`
- [ ] Implementar `PermissionService.validateFeature()`
- [ ] Verificar tabla `features` tiene datos correctos
- [ ] Agregar tests unitarios
- [ ] Agregar tests de integración
- [ ] Probar con token JWT real
- [ ] Verificar logs no muestran errores 400

---

## 🔗 Referencias

- **Frontend Implementation:** `FRONTEND-PERMISSIONS-IMPLEMENTATION.md`
- **Architecture:** `ROLES-SUBSCRIPTION-ARCHITECTURE.md`
- **Backend Checklist:** `BACKEND-CHECKLIST-MICROSERVICIOS.md`
- **Integration Testing:** `INTEGRATION-TESTING-GUIDE.md`

---

## 💡 Workaround Temporal (Si el backend no está listo)

Si el backend NO está implementado aún, puedes hacer que el frontend siempre devuelva `allowed: true`:

```typescript
// En permission.service.ts (TEMPORAL)
canAccess(featureCode: string): Observable<boolean> {
  const token = decodeToken();
  if (!token) return of(false);
  if (this.isSuperAdmin()) return of(true);

  // ⚠️ TEMPORAL: Permitir todo hasta que el backend esté listo
  console.warn('[TEMPORAL] Permitiendo acceso sin validación backend');
  return of(true);
}
```

**⚠️ IMPORTANTE:** Remover este workaround antes de ir a producción.

---

## 📝 Resumen

1. ✅ **Frontend mejorado** con mejor debugging y prevención de toast spam
2. ⏳ **Backend pendiente** de implementar endpoint `/api/permission/validate-feature`
3. 📋 **Documentación** creada con ejemplos de implementación
4. 🎯 **Next steps**: Implementar backend según los DTOs y lógica especificados
