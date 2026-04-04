# 🔴 ISSUE: Error 401 en GET /api/subscriptions/available-companies

**Fecha:** 21 de Diciembre, 2025  
**Severidad:** ALTA - Bloquea funcionalidad crítica  
**Microservicio:** `ebill-billing` (Puerto 8480)

---

## 📋 Descripción del Problema

El endpoint `GET /api/subscriptions/available-companies` está retornando **401 Unauthorized** cuando es llamado por un usuario con rol `super_admin`.

### Request que está fallando:

```bash
curl --location 'http://localhost:8480/api/subscriptions/available-companies' \
--header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJzdXBlcl9hZG1pbiJdLCJ1c2VyTmFtZSI6IjE3MjI2MzM5NjEiLCJjb21wYW55RXN0YWJsaXNobWVudHMiOlt7ImlkQ29tcGFueSI6MSwiZXN0YWJsaXNobWVudHMiOlsxXX0seyJpZENvbXBhbnkiOjIsImVzdGFibGlzaG1lbnRzIjpbXX1dLCJpYXQiOjE3NjYzMjQzNjAsImV4cCI6MTc2NjMyNjE2MH0.-i1HScbEFnMm0sF45-EPI_kDDSa0Aj7ufPq_nfn4ZGw'
```

### Response actual:

```
HTTP 401 Unauthorized
```

### Response esperado:

```json
[
  {
    "idCompany": 3,
    "businessName": "Empresa Sin Suscripción SAS",
    "ruc": "1234567890001"
  }
]
```

---

## 🔍 Diagnóstico

### Posibles causas:

1. **Anotación de seguridad faltante o incorrecta:**

   ```java
   // ❌ MAL - Sin anotación de seguridad
   @GetMapping("/available-companies")
   public ResponseEntity<List<CompanyWithoutSubscriptionDTO>> getCompaniesWithoutSubscription()

   // ✅ CORRECTO - Con anotación @PreAuthorize
   @GetMapping("/available-companies")
   @PreAuthorize("hasRole('SUPER_ADMIN')")
   public ResponseEntity<List<CompanyWithoutSubscriptionDTO>> getCompaniesWithoutSubscription()
   ```

2. **El rol en el token es `super_admin` (con guión bajo) pero el endpoint espera `SUPER_ADMIN` (con guión bajo y mayúsculas)**

   - Token JWT contiene: `"roles":["super_admin"]`
   - Anotación Spring Security usa: `hasRole('SUPER_ADMIN')`
   - Spring Security agrega prefijo `ROLE_` automáticamente

3. **Configuración incorrecta de Spring Security para este endpoint específico**

---

## ✅ Solución Recomendada

### Verificar en el Controller:

```java
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionManagementController {

    @GetMapping("/available-companies")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('super_admin')")  // Aceptar ambas variantes
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
}
```

### O ajustar la configuración de seguridad global:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults(""); // Eliminar prefijo "ROLE_"
    }
}
```

---

## 🧪 Cómo Verificar que está Corregido

### 1. Probar con cURL:

```bash
curl --location 'http://localhost:8480/api/subscriptions/available-companies' \
--header 'Authorization: Bearer [TU-TOKEN-SUPER-ADMIN]'
```

**Resultado esperado:** `200 OK` con array de empresas (puede ser vacío `[]`)

### 2. Verificar en logs del backend:

```
INFO  [nio-8480-exec-7] c.w.e.c.SubscriptionManagementController :
🔍 SUPER_ADMIN solicitando empresas sin suscripción
INFO  [nio-8480-exec-7] c.w.e.c.SubscriptionManagementController :
✅ Empresas sin suscripción retornadas: 2
```

---

## 📊 Comparación con Otros Endpoints

### ✅ Endpoints que SÍ funcionan (mismo controller):

```java
@GetMapping  // GET /api/subscriptions
@PreAuthorize("hasRole('SUPER_ADMIN')")
public ResponseEntity<List<SubscriptionAdmin>> getAllSubscriptions()
// ✅ Este funciona correctamente
```

```java
@PostMapping  // POST /api/subscriptions
@PreAuthorize("hasRole('SUPER_ADMIN')")
public ResponseEntity<Void> assignSubscription(@RequestBody AssignSubscriptionRequest request)
// ✅ Este funciona correctamente
```

### ❌ Endpoint que NO funciona:

```java
@GetMapping("/available-companies")
// ❓ Verificar si tiene @PreAuthorize
public ResponseEntity<List<CompanyWithoutSubscriptionDTO>> getCompaniesWithoutSubscription()
// ❌ Retorna 401
```

---

## 💡 Recomendación Adicional

Si el endpoint accede a la tabla `company` que pertenece a otro microservicio (`ebill-authentication`), verificar:

1. **Permisos de base de datos:** El usuario de BD tiene acceso a la tabla `company`
2. **Configuración de DataSource:** Si usa múltiples esquemas, verificar que esté configurado correctamente
3. **Query SQL:** Verificar que la consulta no esté generando errores silenciosos

---

## 🔗 Referencias

- **Especificación completa:** `BACKEND-SUBSCRIPTION-ENDPOINTS-SPEC.md` (líneas 20-226)
- **Frontend afectado:** `assign-subscription-dialog.component.ts` (línea 78)
- **Logs de error:** Ver logs del frontend arriba

---

## ✅ Checklist de Verificación

- [ ] Verificar anotación `@PreAuthorize` en el método
- [ ] Verificar que el rol en el token coincida con el esperado
- [ ] Probar endpoint con Postman usando el token real
- [ ] Verificar logs del backend cuando se hace la petición
- [ ] Confirmar que el usuario de BD tiene permisos en tabla `company`
- [ ] Probar que retorne `200 OK` con array vacío `[]` si no hay empresas
- [ ] Probar que retorne `200 OK` con empresas si existen datos

---

**Prioridad:** 🔴 ALTA  
**Bloqueante:** Sí - Sin este endpoint, no se pueden asignar suscripciones manualmente

**Última actualización:** 21/12/2025 20:50
