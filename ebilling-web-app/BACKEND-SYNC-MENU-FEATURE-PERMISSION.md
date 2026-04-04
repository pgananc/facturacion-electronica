# 🔄 Sincronización Automática: Menu → Feature Permission

## 📋 Contexto

**Problema:** Cuando se crea un nuevo menú con `feature_code` desde la UI de administración, el `feature_code` se guarda en la tabla `menu` pero NO se sincroniza automáticamente con la tabla `feature_permission`. Esto causa que:

- ❌ El nuevo menú aparece en el menú lateral
- ❌ Pero NO aparece en las matrices de permisos (roles/planes)
- ❌ No se pueden asignar permisos hasta que se agregue manualmente a `feature_permission`

**Solución:** Implementar sincronización automática entre `menu.feature_code` y `feature_permission`.

---

## 🎯 Objetivo

**Sincronizar automáticamente** los `feature_code` de la tabla `menu` con la tabla `feature_permission` para que:

- ✅ Al crear/editar un menú con `feature_code` → Se cree automáticamente en `feature_permission`
- ✅ Las matrices de permisos se actualicen automáticamente
- ✅ NO se requiera deploy del frontend
- ✅ NO se requiera intervención manual

---

## 🏗️ Arquitectura de Solución

### **Opción 1: Sincronización al Crear/Editar Menú** ⭐ RECOMENDADA

**Cuándo se ejecuta:**

- Al crear un menú con `feature_code`
- Al editar un menú y agregar/cambiar `feature_code`
- Al eliminar un menú (opcional: marcar feature como inactiva)

**Ventajas:**

- ✅ Sincronización inmediata
- ✅ Siempre consistente
- ✅ Sin procesos adicionales

**Implementación:**

```java
@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private FeaturePermissionRepository featurePermissionRepository;

    public Menu createMenu(MenuRequest request) {
        Menu menu = new Menu();
        menu.setName(request.getName());
        menu.setUrl(request.getUrl());
        menu.setIcon(request.getIcon());
        menu.setFeatureCode(request.getFeatureCode());
        menu.setParentId(request.getParentId());
        menu.setActive(true);

        Menu savedMenu = menuRepository.save(menu);

        // ⭐ SINCRONIZAR CON feature_permission
        if (savedMenu.getFeatureCode() != null && !savedMenu.getFeatureCode().isEmpty()) {
            syncFeaturePermission(savedMenu.getFeatureCode(), savedMenu.getName());
        }

        return savedMenu;
    }

    public Menu updateMenu(Long id, MenuRequest request) {
        Menu menu = menuRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Menú no encontrado"));

        String oldFeatureCode = menu.getFeatureCode();
        String newFeatureCode = request.getFeatureCode();

        menu.setName(request.getName());
        menu.setUrl(request.getUrl());
        menu.setIcon(request.getIcon());
        menu.setFeatureCode(newFeatureCode);
        menu.setParentId(request.getParentId());

        Menu savedMenu = menuRepository.save(menu);

        // ⭐ SINCRONIZAR SI CAMBIÓ EL feature_code
        if (newFeatureCode != null && !newFeatureCode.isEmpty()) {
            if (!newFeatureCode.equals(oldFeatureCode)) {
                // Feature code cambió, sincronizar
                syncFeaturePermission(newFeatureCode, savedMenu.getName());
            } else {
                // Feature code igual, solo actualizar nombre si cambió
                updateFeaturePermissionName(newFeatureCode, savedMenu.getName());
            }
        }

        return savedMenu;
    }

    /**
     * Sincronizar feature_code con feature_permission
     */
    private void syncFeaturePermission(String featureCode, String featureName) {
        FeaturePermission existing = featurePermissionRepository
            .findByFeatureCode(featureCode)
            .orElse(null);

        if (existing == null) {
            // Crear nuevo registro en feature_permission
            FeaturePermission newFeature = new FeaturePermission();
            newFeature.setFeatureCode(featureCode);
            newFeature.setFeatureName(featureName);
            newFeature.setModule(detectModule(featureCode));
            newFeature.setAllowedRoles(new int[]{}); // Vacío por defecto
            newFeature.setRequiredPlans(new String[]{}); // Vacío por defecto
            newFeature.setIsActive(true);
            newFeature.setDescription("Creado automáticamente desde menú");

            featurePermissionRepository.save(newFeature);

            log.info("✅ Feature '{}' creado automáticamente en feature_permission", featureCode);
        } else {
            // Ya existe, actualizar nombre si cambió
            if (!existing.getFeatureName().equals(featureName)) {
                existing.setFeatureName(featureName);
                featurePermissionRepository.save(existing);
                log.debug("✅ Feature '{}' actualizado en feature_permission", featureCode);
            }
        }
    }

    /**
     * Actualizar solo el nombre de un feature existente
     */
    private void updateFeaturePermissionName(String featureCode, String featureName) {
        FeaturePermission existing = featurePermissionRepository
            .findByFeatureCode(featureCode)
            .orElse(null);

        if (existing != null && !existing.getFeatureName().equals(featureName)) {
            existing.setFeatureName(featureName);
            featurePermissionRepository.save(existing);
        }
    }

    /**
     * Detectar módulo basado en feature_code
     */
    private String detectModule(String featureCode) {
        if (featureCode.startsWith("REPORT_")) {
            return "REPORTS";
        } else if (featureCode.startsWith("INVOICE_") || featureCode.startsWith("CREDIT_NOTE_")) {
            return "BILLING";
        } else if (featureCode.startsWith("CLIENT_")) {
            return "CLIENTS";
        } else if (featureCode.startsWith("PRODUCT_")) {
            return "PRODUCTS";
        } else if (featureCode.startsWith("USER_") || featureCode.startsWith("COMPANY_")) {
            return "ADMINISTRATION";
        } else if (featureCode.startsWith("ADMIN_") || featureCode.contains("MANAGEMENT")) {
            return "ADMINISTRATION";
        } else {
            return "GENERAL";
        }
    }
}
```

---

### **Opción 2: Endpoint de Sincronización Manual**

**Cuándo se ejecuta:**

- Manualmente desde la UI de administración
- Como tarea programada (cron job)

**Ventajas:**

- ✅ Control explícito
- ✅ Útil para migraciones masivas

**Implementación:**

```java
@RestController
@RequestMapping("/api/admin/menus")
public class MenuAdminController {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private FeaturePermissionRepository featurePermissionRepository;

    /**
     * Sincronizar todos los menús con feature_code a feature_permission
     */
    @PostMapping("/sync-features")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SyncFeaturesResponse> syncMenuFeatures() {
        List<Menu> menusWithFeatureCode = menuRepository
            .findByFeatureCodeIsNotNullAndActiveTrue();

        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (Menu menu : menusWithFeatureCode) {
            FeaturePermission existing = featurePermissionRepository
                .findByFeatureCode(menu.getFeatureCode())
                .orElse(null);

            if (existing == null) {
                // Crear nuevo
                FeaturePermission newFeature = new FeaturePermission();
                newFeature.setFeatureCode(menu.getFeatureCode());
                newFeature.setFeatureName(menu.getName());
                newFeature.setModule(detectModule(menu.getFeatureCode()));
                newFeature.setAllowedRoles(new int[]{});
                newFeature.setRequiredPlans(new String[]{});
                newFeature.setIsActive(true);
                featurePermissionRepository.save(newFeature);
                created++;
            } else {
                // Actualizar nombre si cambió
                if (!existing.getFeatureName().equals(menu.getName())) {
                    existing.setFeatureName(menu.getName());
                    featurePermissionRepository.save(existing);
                    updated++;
                } else {
                    skipped++;
                }
            }
        }

        SyncFeaturesResponse response = new SyncFeaturesResponse();
        response.setCreated(created);
        response.setUpdated(updated);
        response.setSkipped(skipped);
        response.setTotal(menusWithFeatureCode.size());

        return ResponseEntity.ok(response);
    }
}
```

---

### **Opción 3: Query Dinámica en Endpoint de Features** ⭐ COMPLEMENTARIA

**Modificar endpoint `/api/permission/permissions/features`** para que obtenga features directamente de los menús:

```java
@GetMapping("/api/permission/permissions/features")
public ResponseEntity<List<FeaturePermissionResponse>> getAllFeatures() {
    // Obtener features desde menús (fuente de verdad)
    List<Menu> menusWithFeatureCode = menuRepository
        .findByFeatureCodeIsNotNullAndActiveTrue();

    List<FeaturePermissionResponse> features = menusWithFeatureCode.stream()
        .map(menu -> {
            // Buscar permisos en feature_permission
            FeaturePermission permission = featurePermissionRepository
                .findByFeatureCode(menu.getFeatureCode())
                .orElse(null);

            FeaturePermissionResponse response = new FeaturePermissionResponse();
            response.setFeatureCode(menu.getFeatureCode());
            response.setFeatureName(menu.getName()); // Priorizar nombre del menú
            response.setModule(permission != null ? permission.getModule() : detectModule(menu.getFeatureCode()));
            response.setAllowedRoles(permission != null ? permission.getAllowedRoles() : new int[]{});
            response.setRequiredPlans(permission != null ? permission.getRequiredPlans() : new String[]{});
            response.setDescription(permission != null ? permission.getDescription() : "");
            response.setIsActive(permission != null ? permission.getIsActive() : true);

            return response;
        })
        .collect(Collectors.toList());

    return ResponseEntity.ok(features);
}
```

**Ventajas:**

- ✅ Features siempre sincronizados con menús
- ✅ Si un menú tiene `feature_code`, aparece automáticamente
- ✅ Si no existe en `feature_permission`, se muestra con valores por defecto

---

## 🎯 Solución Recomendada: Combinación

**Implementar Opción 1 + Opción 3:**

1. **Opción 1:** Sincronización automática al crear/editar menú

   - Garantiza que `feature_permission` siempre esté actualizado
   - Permisos se pueden asignar inmediatamente

2. **Opción 3:** Endpoint obtiene features del menú
   - Fuente de verdad es la tabla `menu`
   - Si falta sincronización, el endpoint lo compensa

**Resultado:**

- ✅ Sin deploy del frontend necesario
- ✅ Features siempre sincronizados
- ✅ Matrices de permisos se actualizan automáticamente
- ✅ Escalable: agregar menús sin tocar código

---

## 📋 Checklist de Implementación

### Backend (ebill-authentication)

- [ ] Modificar `MenuService.createMenu()` para sincronizar automáticamente
- [ ] Modificar `MenuService.updateMenu()` para sincronizar si cambió `feature_code`
- [ ] Implementar método `syncFeaturePermission()`
- [ ] Implementar método `detectModule()` para detectar módulo automáticamente
- [ ] Modificar endpoint `/api/permission/permissions/features` para obtener features del menú
- [ ] (Opcional) Crear endpoint `/api/admin/menus/sync-features` para sincronización manual
- [ ] Agregar logs para debugging
- [ ] Agregar tests unitarios

### Validaciones

- [ ] Validar que `feature_code` sea único antes de crear
- [ ] Validar que solo menús hoja pueden tener `feature_code`
- [ ] Manejar casos edge (feature_code vacío, null, etc.)

### Testing

- [ ] Test: Crear menú con `feature_code` → Verificar que se crea en `feature_permission`
- [ ] Test: Editar menú y cambiar `feature_code` → Verificar sincronización
- [ ] Test: Crear menú sin `feature_code` → No debe crear en `feature_permission`
- [ ] Test: Endpoint `/api/permission/permissions/features` retorna features del menú

---

## 🔍 SQL de Verificación

```sql
-- Verificar sincronización: Menús con feature_code que NO están en feature_permission
SELECT
    m.id,
    m.name,
    m.url,
    m.feature_code,
    '❌ NO SINCRONIZADO' as estado
FROM menu m
WHERE m.active = true
  AND m.feature_code IS NOT NULL
  AND m.feature_code != ''
  AND NOT EXISTS (
      SELECT 1
      FROM feature_permission fp
      WHERE fp.feature_code = m.feature_code
  )
ORDER BY m.id;

-- Verificar sincronización: Feature_permission que NO tienen menú activo
SELECT
    fp.feature_code,
    fp.feature_name,
    '⚠️ MENÚ INACTIVO O ELIMINADO' as estado
FROM feature_permission fp
WHERE fp.is_active = true
  AND NOT EXISTS (
      SELECT 1
      FROM menu m
      WHERE m.feature_code = fp.feature_code
        AND m.active = true
  )
ORDER BY fp.feature_code;
```

---

## 📝 Notas Importantes

1. **Valores por defecto:** Cuando se crea automáticamente en `feature_permission`:

   - `allowed_roles`: `[]` (vacío - requiere asignación manual)
   - `required_plans`: `[]` (vacío - requiere asignación manual)
   - `is_active`: `true`

2. **Actualización de nombre:** Si el nombre del menú cambia, se actualiza automáticamente en `feature_permission`

3. **Detección de módulo:** Se detecta automáticamente basado en el prefijo del `feature_code`

4. **Eliminación de menú:** (Opcional) Cuando se elimina un menú, se puede marcar el feature como inactivo en lugar de eliminarlo

---

**Estado:** ⚠️ Pendiente de implementar  
**Prioridad:** Media-Alta (necesario para escalabilidad)  
**Dependencias:** Página de admin para menús
