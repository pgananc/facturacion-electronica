# 🔄 Endpoints Backend: Sincronización Automática Menu ↔ Feature Permission

## 📋 Contexto

El frontend ahora gestiona automáticamente la creación/sincronización de `feature_permission` cuando se crea o edita un menú con `featureCode`. 

**⚠️ IMPORTANTE:** Estos endpoints son **OPCIONALES** pero **RECOMENDADOS** para mejorar la experiencia de usuario. El frontend funciona correctamente sin ellos, pero la UX será mejor si están implementados.

**Comportamiento actual del backend:**
- El backend valida que el `featureCode` exista y esté activo en `feature_permission`
- Si no existe, devuelve error 400: "Feature code no encontrado o inactivo"
- El backend devuelve el objeto `feature` completo en las respuestas

**Con estos endpoints opcionales:**
- El frontend puede verificar si un feature existe antes de guardar
- El frontend puede crear automáticamente el feature si no existe
- Mejor UX: el usuario ve feedback en tiempo real

---

## 🎯 Endpoints Requeridos

### 1. **Verificar si un Feature Existe**

```
GET /api/admin/menu/check-feature/{featureCode}
```

**Descripción:** Verifica si un `featureCode` ya existe en la tabla `feature_permission`.

**Headers:**
```
Authorization: Bearer {JWT}
```

**Response 200:**
```json
{
  "exists": true
}
```

o

```json
{
  "exists": false
}
```

**Implementación Java (Ejemplo):**

```java
@GetMapping("/check-feature/{featureCode}")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public ResponseEntity<Map<String, Boolean>> checkFeatureExists(
    @PathVariable String featureCode
) {
    boolean exists = featurePermissionRepository
        .findByFeatureCode(featureCode)
        .isPresent();
    
    Map<String, Boolean> response = new HashMap<>();
    response.put("exists", exists);
    
    return ResponseEntity.ok(response);
}
```

---

### 2. **Sincronizar Feature Permission**

```
POST /api/admin/menu/sync-feature
```

**Descripción:** Crea o actualiza un registro en `feature_permission` basado en el `featureCode` y `featureName` del menú.

**Headers:**
```
Authorization: Bearer {JWT}
Content-Type: application/json
```

**Request Body:**
```json
{
  "featureCode": "INVOICE_CREATE",
  "featureName": "Crear Facturas"
}
```

**Response 200:**
```
204 No Content
```

o

```json
{
  "message": "Feature creado/actualizado exitosamente",
  "created": true  // true si se creó, false si se actualizó
}
```

**Implementación Java (Ejemplo):**

```java
@PostMapping("/sync-feature")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public ResponseEntity<Map<String, Object>> syncFeaturePermission(
    @RequestBody SyncFeatureRequest request
) {
    String featureCode = request.getFeatureCode();
    String featureName = request.getFeatureName();
    
    Optional<FeaturePermission> existing = featurePermissionRepository
        .findByFeatureCode(featureCode);
    
    boolean created = false;
    
    if (existing.isPresent()) {
        // Actualizar feature existente
        FeaturePermission feature = existing.get();
        if (!feature.getFeatureName().equals(featureName)) {
            feature.setFeatureName(featureName);
            featurePermissionRepository.save(feature);
        }
    } else {
        // Crear nuevo feature
        FeaturePermission newFeature = new FeaturePermission();
        newFeature.setFeatureCode(featureCode);
        newFeature.setFeatureName(featureName);
        newFeature.setModule(detectModule(featureCode));
        newFeature.setAllowedRoles(new int[]{}); // Vacío por defecto
        newFeature.setRequiredPlans(new String[]{}); // Vacío por defecto
        newFeature.setIsActive(true);
        newFeature.setDescription("Creado automáticamente desde menú");
        
        featurePermissionRepository.save(newFeature);
        created = true;
    }
    
    Map<String, Object> response = new HashMap<>();
    response.put("message", created ? "Feature creado exitosamente" : "Feature actualizado exitosamente");
    response.put("created", created);
    
    return ResponseEntity.ok(response);
}

/**
 * Detectar módulo basado en feature_code
 */
private String detectModule(String featureCode) {
    if (featureCode.startsWith("INVOICE") || featureCode.startsWith("CREDIT_NOTE") || featureCode.startsWith("DEBIT_NOTE")) {
        return "BILLING";
    } else if (featureCode.startsWith("REPORT")) {
        return "REPORTS";
    } else if (featureCode.startsWith("CLIENT") || featureCode.startsWith("CUSTOMER")) {
        return "CUSTOMER";
    } else if (featureCode.startsWith("PRODUCT")) {
        return "PRODUCT";
    } else if (featureCode.startsWith("ADMIN") || featureCode.startsWith("MENU") || featureCode.startsWith("PERMISSION") || featureCode.startsWith("SUBSCRIPTION")) {
        return "ADMIN";
    } else if (featureCode.startsWith("DASHBOARD")) {
        return "DASHBOARD";
    } else {
        return "GENERAL";
    }
}
```

**DTO Request:**

```java
public class SyncFeatureRequest {
    private String featureCode;
    private String featureName;
    
    // Getters y setters
    public String getFeatureCode() { return featureCode; }
    public void setFeatureCode(String featureCode) { this.featureCode = featureCode; }
    
    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }
}
```

---

## 🔄 Flujo Completo

### Escenario 1: Crear Menú con FeatureCode Nuevo

1. Usuario ingresa `featureCode` en el formulario
2. Frontend llama a `GET /api/admin/menu/check-feature/{featureCode}`
3. Backend responde `{ "exists": false }`
4. Frontend muestra: "⚠️ Feature no existe. Se creará automáticamente al guardar."
5. Usuario hace click en "Crear"
6. Frontend llama a `POST /api/admin/menu` (crear menú)
7. Frontend llama a `POST /api/admin/menu/sync-feature` (crear feature_permission)
8. Backend crea el registro en `feature_permission` con valores por defecto
9. Frontend muestra: "Menú creado y feature sincronizado"

### Escenario 2: Crear Menú con FeatureCode Existente

1. Usuario ingresa `featureCode` en el formulario
2. Frontend llama a `GET /api/admin/menu/check-feature/{featureCode}`
3. Backend responde `{ "exists": true }`
4. Frontend muestra: "✅ Feature existe en feature_permission"
5. Usuario hace click en "Crear"
6. Frontend llama a `POST /api/admin/menu` (crear menú)
7. Frontend llama a `POST /api/admin/menu/sync-feature` (actualizar feature_permission si el nombre cambió)
8. Backend actualiza `feature_name` si es diferente
9. Frontend muestra: "Menú creado y feature sincronizado"

### Escenario 3: Editar Menú y Cambiar FeatureCode

1. Usuario edita un menú y cambia el `featureCode`
2. Frontend valida el nuevo `featureCode` en tiempo real
3. Si el nuevo `featureCode` no existe, muestra advertencia
4. Al guardar, sincroniza el nuevo `featureCode` con `feature_permission`

---

## 🔒 Seguridad

### Validaciones Requeridas

1. ✅ Usuario autenticado (JWT válido)
2. ✅ Usuario tiene rol `SUPER_ADMIN` (id=1)
3. ✅ `featureCode` no está vacío (en sync-feature)
4. ✅ `featureName` no está vacío (en sync-feature)
5. ✅ `featureCode` tiene formato válido (solo mayúsculas, números, guiones bajos)

### Validación de Formato (Opcional pero Recomendado)

```java
private boolean isValidFeatureCode(String featureCode) {
    // Solo mayúsculas, números y guiones bajos
    return featureCode.matches("^[A-Z0-9_]+$") && 
           featureCode.length() <= 100;
}
```

---

## 📊 Base de Datos

### Tabla `feature_permission`

```sql
CREATE TABLE IF NOT EXISTS public.feature_permission (
  id_permission SERIAL PRIMARY KEY,
  feature_code VARCHAR(100) NOT NULL UNIQUE,
  feature_name VARCHAR(255) NOT NULL,
  description TEXT,
  module VARCHAR(50),
  allowed_roles INT[] DEFAULT '{}',
  required_plans VARCHAR(50)[] DEFAULT '{}',
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_feature_permission_code ON public.feature_permission(feature_code);
```

---

## ✅ Checklist de Implementación Backend

- [ ] Endpoint `GET /api/admin/menu/check-feature/{featureCode}` implementado
- [ ] Endpoint `POST /api/admin/menu/sync-feature` implementado
- [ ] Validación de rol SUPER_ADMIN en ambos endpoints
- [ ] Validación de formato de `featureCode`
- [ ] Detección automática de módulo basado en `featureCode`
- [ ] Actualización de `feature_name` si cambia el nombre del menú
- [ ] Manejo de errores (feature_code duplicado, etc.)
- [ ] Logs de auditoría (quién creó/actualizó el feature)
- [ ] Tests unitarios para ambos endpoints

---

## 🚀 Ventajas de Esta Solución

1. ✅ **UX Mejorada:** El usuario ve en tiempo real si el feature existe o no
2. ✅ **Sincronización Automática:** No requiere intervención manual
3. ✅ **Consistencia:** El `feature_name` siempre coincide con el nombre del menú
4. ✅ **Retrocompatibilidad:** Si los endpoints no existen, el frontend maneja el error gracefully
5. ✅ **Flexibilidad:** El usuario puede crear features nuevos directamente desde el formulario de menús

---

## 📝 Notas Adicionales

- Si el backend prefiere manejar la sincronización automáticamente al crear/editar el menú (sin endpoints separados), puede hacerlo. En ese caso, estos endpoints serían opcionales y el frontend simplemente no los llamaría.

- El frontend está diseñado para funcionar incluso si estos endpoints no existen (retrocompatibilidad), pero la experiencia de usuario será mejor si están implementados.

