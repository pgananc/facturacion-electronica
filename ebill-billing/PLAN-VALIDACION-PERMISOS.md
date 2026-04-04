# 📋 Plan: Validación de Permisos - Endpoints de Permisos y Planes

## 🎯 Objetivo

Implementar validaciones de permisos en el backend para endpoints relacionados con permisos y planes de suscripción.

---

## 🔍 Análisis de Endpoints Actuales

### Endpoints Propuestos (con path actual)

| Método | Endpoint Actual                              | Acceso Requerido           | Problema                                      |
| ------ | -------------------------------------------- | -------------------------- | --------------------------------------------- |
| GET    | `/api/permission/permissions/features/plans` | Cualquier rol con permisos | ❌ Path redundante (`permission/permissions`) |
| PUT    | `/api/permission/permissions/plans/bulk`     | Solo `SUPER_ADMIN`         | ❌ Path redundante                            |
| GET    | `/api/permission/permissions/features`       | Cualquier rol con permisos | ❌ Path redundante                            |
| PUT    | `/api/permission/permissions/bulk`           | Solo `SUPER_ADMIN`         | ❌ Path redundante                            |

### Problemas Identificados

1. **Path redundante:** `/api/permission/permissions/...` (permission aparece dos veces)
2. **Falta claridad:** No queda claro qué hace cada endpoint
3. **Inconsistencia:** Mezcla de singular/plural (`permission` vs `permissions`)

---

## 💡 Propuesta de Estructura de Paths

### Opción 1: Estructura RESTful Clara (Recomendada)

```
GET  /api/permissions/features/plans          → Ver features de planes
PUT  /api/permissions/plans/bulk             → Actualizar planes (bulk)
GET  /api/permissions/features               → Ver todas las features
PUT  /api/permissions/bulk                    → Actualizar permisos (bulk)
```

**Ventajas:**

- ✅ Sin redundancia
- ✅ RESTful y claro
- ✅ Consistente con otros endpoints (`/api/subscriptions`, `/api/invoice`)

### Opción 2: Estructura más específica

```
GET  /api/permissions/features/plans          → Ver features de planes
PUT  /api/admin/permissions/plans/bulk        → Actualizar planes (bulk) - admin
GET  /api/permissions/features                → Ver todas las features
PUT  /api/admin/permissions/bulk              → Actualizar permisos (bulk) - admin
```

**Ventajas:**

- ✅ Separa endpoints administrativos (`/admin/`)
- ✅ Más explícito sobre quién puede acceder

### Opción 3: Estructura por recurso

```
GET  /api/plan-features                       → Ver features de planes
PUT  /api/plan-features/bulk                  → Actualizar features de planes (bulk)
GET  /api/features                            → Ver todas las features
PUT  /api/features/bulk                       → Actualizar features (bulk)
```

**Ventajas:**

- ✅ Más simple y directo
- ✅ Cada recurso tiene su propio path

---

## 🎯 Recomendación: Opción 1

**Estructura propuesta:**

```
GET  /api/permissions/features/plans          → Ver features de planes
PUT  /api/permissions/plans/bulk             → Actualizar planes (bulk)
GET  /api/permissions/features                → Ver todas las features
PUT  /api/permissions/bulk                    → Actualizar permisos (bulk)
```

**Razones:**

- Consistente con `/api/subscriptions`, `/api/invoice`
- Sin redundancia
- RESTful y claro
- Fácil de entender y mantener

---

## 🔐 Estrategia de Validación

### ✅ Validación Manual con JWT (Como se usa actualmente)

**Contexto:**

- ✅ NO usamos Spring Security `@PreAuthorize`
- ✅ Usamos JWT directamente con `ebill-auth-util`
- ✅ Los roles vienen en el token JWT
- ✅ Validación manual usando `SecurityContextHolder` (como en `SubscriptionServiceImpl.isSuperAdmin()`)

**Implementación:**

```java
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * GET /api/permissions/features/plans
     * Cualquier usuario autenticado con un rol puede ver
     */
    @GetMapping("/features/plans")
    public ResponseEntity<?> getPlanFeatures() {
        // Validar que esté autenticado y tenga un rol
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        // Verificar que tenga al menos un rol (authority)
        if (auth.getAuthorities() == null || auth.getAuthorities().isEmpty()) {
            throw new IllegalStateException("Usuario sin roles asignados");
        }
        return ResponseEntity.ok(permissionService.getPlanFeatures());
    }

    /**
     * PUT /api/permissions/plans/bulk
     * Solo SUPER_ADMIN puede actualizar
     */
    @PutMapping("/plans/bulk")
    public ResponseEntity<?> updatePlansBulk(@RequestBody BulkUpdateRequest request) {
        if (!isSuperAdmin()) {
            throw new IllegalStateException("Se requiere rol SUPER_ADMIN para esta operación");
        }
        return ResponseEntity.ok(permissionService.updatePlansBulk(request));
    }

    /**
     * GET /api/permissions/features
     * Cualquier usuario autenticado con un rol puede ver
     */
    @GetMapping("/features")
    public ResponseEntity<?> getFeatures() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Usuario no autenticado");
        }
        if (auth.getAuthorities() == null || auth.getAuthorities().isEmpty()) {
            throw new IllegalStateException("Usuario sin roles asignados");
        }
        return ResponseEntity.ok(permissionService.getFeatures());
    }

    /**
     * PUT /api/permissions/bulk
     * Solo SUPER_ADMIN puede actualizar
     */
    @PutMapping("/bulk")
    public ResponseEntity<?> updatePermissionsBulk(@RequestBody BulkUpdateRequest request) {
        if (!isSuperAdmin()) {
            throw new IllegalStateException("Se requiere rol SUPER_ADMIN para esta operación");
        }
        return ResponseEntity.ok(permissionService.updatePermissionsBulk(request));
    }

    /**
     * Método helper para validar SUPER_ADMIN (igual que SubscriptionServiceImpl)
     */
    private boolean isSuperAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getAuthorities() == null) {
                return false;
            }
            return authentication.getAuthorities().stream()
                .anyMatch(auth -> {
                    String authority = auth.getAuthority();
                    return "ROLE_SUPER_ADMIN".equals(authority)
                        || "SUPER_ADMIN".equals(authority)
                        || "ROLE_[super_admin]".equals(authority)
                        || "super_admin".equalsIgnoreCase(authority)
                        || "ROLE_super_admin".equalsIgnoreCase(authority);
                });
        } catch (Exception e) {
            log.error("Error al verificar SUPER_ADMIN: {}", e.getMessage());
            return false;
        }
    }
}
```

---

## ✅ Decisión: Validación Manual con JWT

**Razones:**

- ✅ Consistente con el código actual (`SubscriptionServiceImpl.isSuperAdmin()`)
- ✅ Usamos JWT directamente, no Spring Security Method Security
- ✅ Los roles vienen en el token JWT validado por `ebill-auth-util`
- ✅ Más control sobre mensajes de error personalizados

---

## 📝 Plan de Implementación

### Fase 1: Estructura Base

1. **Crear `PermissionController.java`**

   - Path base: `/api/permissions`
   - Métodos básicos con validaciones

2. **Crear `PermissionService.java`** (si no existe)
   - Lógica de negocio
   - Validaciones adicionales si es necesario

### Fase 2: Validaciones

1. **Crear método helper `isSuperAdmin()`** (reutilizar lógica de `SubscriptionServiceImpl`)

   - O crear utilidad compartida `SecurityUtils`

2. **Implementar validaciones manuales:**
   - GET endpoints: Verificar `authentication.isAuthenticated()` y que tenga authorities
   - PUT endpoints: Llamar a `isSuperAdmin()` y lanzar excepción si no es admin

### Fase 3: Manejo de Errores

1. **Crear excepciones personalizadas:**

   - `PermissionDeniedException` (403)
   - `UnauthorizedException` (401)

2. **Actualizar `GlobalExceptionHandler`:**
   - Manejar `AccessDeniedException` de Spring Security
   - Retornar 403 con mensaje claro

---

## 🔧 Configuración Necesaria

### 1. Crear Utilidad Compartida para Validación (Recomendado)

**Opción A: Crear `SecurityUtils` compartido**

```java
@Component
@Slf4j
public class SecurityUtils {

    /**
     * Verifica si el usuario actual es SUPER_ADMIN
     * Reutiliza la lógica de SubscriptionServiceImpl
     */
    public boolean isSuperAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getAuthorities() == null) {
                return false;
            }
            return authentication.getAuthorities().stream()
                .anyMatch(auth -> {
                    String authority = auth.getAuthority();
                    return "ROLE_SUPER_ADMIN".equals(authority)
                        || "SUPER_ADMIN".equals(authority)
                        || "ROLE_[super_admin]".equals(authority)
                        || "super_admin".equalsIgnoreCase(authority)
                        || "ROLE_super_admin".equalsIgnoreCase(authority);
                });
        } catch (Exception e) {
            log.error("Error al verificar SUPER_ADMIN: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si el usuario está autenticado y tiene al menos un rol
     */
    public boolean isAuthenticatedWithRole() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null
                && auth.isAuthenticated()
                && auth.getAuthorities() != null
                && !auth.getAuthorities().isEmpty();
        } catch (Exception e) {
            log.error("Error al verificar autenticación: {}", e.getMessage());
            return false;
        }
    }
}
```

**Opción B: Reutilizar método de `SubscriptionServiceImpl`**

- Copiar método `isSuperAdmin()` a cada controller que lo necesite
- Menos mantenible pero más simple

---

## 📋 Checklist de Implementación

- [x] ✅ Decidir estructura de paths: `/api/permissions/...` (Opción 1)
- [ ] Crear `PermissionController.java` con 4 endpoints
- [ ] Crear `PermissionService.java` con lógica de negocio
- [ ] Crear DTOs necesarios (BulkUpdateRequest, etc.)
- [ ] Crear `SecurityUtils` compartido (o reutilizar método de SubscriptionServiceImpl)
- [ ] Implementar validaciones manuales:
  - GET: Verificar autenticación + tener authorities
  - PUT: Verificar `isSuperAdmin()`
- [ ] Crear excepciones personalizadas (opcional, puede usar `IllegalStateException`)
- [ ] Actualizar `GlobalExceptionHandler` si es necesario
- [ ] Agregar logs apropiados
- [ ] Documentar endpoints en Swagger/OpenAPI

---

## ✅ Respuestas Confirmadas

1. **¿Estos endpoints son de `ebill-billing` o de `ebill-authentication`?**

   - ✅ **ebill-billing** - Plan = Subscription Plan

2. **¿Qué significa "cualquier rol con permisos"?**

   - ✅ **Cualquier usuario autenticado que tenga un rol** (no importa cuál)
   - Solo verificar que `authentication.isAuthenticated()` y que tenga `authorities`

3. **¿Los endpoints ya existen o hay que crearlos?**

   - ✅ **Hay que crearlos** - No existen aún

4. **¿Prefieres anotaciones o validación manual?**
   - ✅ **Validación manual** - No usamos Spring Security `@PreAuthorize`
   - Usamos JWT directamente con `ebill-auth-util`
   - Los roles vienen en el token JWT

---

## 🎯 Próximos Pasos

1. **Confirmar estructura de paths** (Opción 1 recomendada)
2. **Confirmar si endpoints son de este microservicio**
3. **Decidir estrategia de validación** (Anotaciones vs Manual)
4. **Implementar según decisiones**

---

## 📚 Referencias

- Código actual: `SubscriptionServiceImpl.isSuperAdmin()`
- Código actual: `SubscriptionManagementController` (ejemplo de endpoints admin)
- Spring Security: `@PreAuthorize` documentation
- ebill-auth-util: Manejo de roles en JWT
