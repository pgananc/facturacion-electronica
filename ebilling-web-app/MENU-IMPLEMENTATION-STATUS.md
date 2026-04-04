# 🔄 Estado Actual: Sistema de Menús con Feature Code

## ✅ IMPLEMENTACIÓN COMPLETA

### Backend ✅ LISTO

- [x] Campo `feature_code` agregado a tabla `menu`
- [x] FK constraint hacia `feature_permission`
- [x] Migración Flyway creada
- [x] Entidad `Menu.java` actualizada
- [x] DTO `MenuDto.java` actualizado
- [x] Mapper `MenuMapper.java` actualizado
- [x] Service actualizado para extraer `featureCode`
- [x] Script de validación SQL creado

### Frontend ✅ LISTO

- [x] Interface `Menu` tiene campo `featureCode?: string`
- [x] `MenuFilterService` prioriza `featureCode` del backend
- [x] Fallback a mapeo URL si backend no lo envía
- [x] `PermissionGuard` valida con `/api/permission/validate-feature`
- [x] Logs de debug activados

---

## 🧪 Testing End-to-End

### Paso 1: Iniciar Backend

```bash
# La migración Flyway se ejecuta automáticamente
./mvnw spring-boot:run

# Verificar que la migración se ejecutó
# Debe aparecer en logs:
# Flyway migration V2025121701__add_feature_code_to_menu.sql applied successfully
```

### Paso 2: Validar Base de Datos

```bash
# Ejecutar script de validación
psql postgresql://postgres:postgres@localhost:5433/ebill-dev-auth \
  -f scripts/validate-menu-feature-integrity.sql

# Debe mostrar:
# ✅ Todos los checks pasaron
```

### Paso 3: Probar Endpoint de Menú

```bash
# Obtener menú con feature codes
curl -X GET http://localhost:8280/api/menu/0102030405 \
  -H "Authorization: Bearer <token>" \
  | jq '.[].subMenus[] | {name, url, featureCode}'
```

**Response esperado:**

```json
{
  "name": "Facturas",
  "url": "/billing",
  "featureCode": "INVOICE_CREATE"
}
{
  "name": "Nota de crédito",
  "url": "/credit-note",
  "featureCode": "CREDIT_NOTE_CREATE"
}
{
  "name": "Gestionar clientes",
  "url": "/customer",
  "featureCode": "CLIENT_CREATE"
}
```

### Paso 4: Frontend - Recargar Aplicación

```bash
# Si el frontend ya está corriendo:
# Solo recarga la página: Cmd+R

# Ver logs en Console (DevTools):
📋 [MenuFilter] Menús recibidos del backend: X grupos
📋 [MenuFilter] Estructura: (X) [{…}]
  0: {id: 1, name: "...", subMenus: [{featureCode: "INVOICE_CREATE"}]}  👈 Debe aparecer
✅ [MenuFilter] Menús después de filtrar: Y grupos
```

### Paso 5: Verificar Filtrado

**Con usuario plan FREE:**

```
✅ Debe ver:
- Facturas (INVOICE_CREATE)
- Clientes (CLIENT_CREATE)
- Compañías (COMPANY_SETTINGS)

❌ NO debe ver:
- Notas de Crédito (CREDIT_NOTE_CREATE - requiere BASIC+)
- Usuarios (USER_MANAGEMENT - requiere BASIC+)
- Reportes Avanzados (REPORT_ADVANCED - requiere PRO+)
```

**Con usuario plan BASIC:**

```
✅ Debe ver todo lo de FREE más:
- Notas de Crédito
- Usuarios
- Establecimientos

❌ NO debe ver:
- Reportes Avanzados (requiere PRO+)
```

---

## 🐛 Troubleshooting

### Problema 1: Backend no devuelve `featureCode`

**Síntoma:**

```json
{
  "name": "Facturas",
  "url": "/billing",
  "featureCode": null // ❌ NULL
}
```

**Causa posible:**

- Migración no se ejecutó
- Datos no se poblaron

**Solución:**

```sql
-- Verificar que la columna existe
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'menu' AND column_name = 'feature_code';

-- Verificar que hay datos
SELECT id, name, url, feature_code
FROM menu
WHERE active = true AND url IS NOT NULL;

-- Si están NULL, ejecutar manualmente:
UPDATE menu SET feature_code = 'INVOICE_CREATE' WHERE url = '/billing';
UPDATE menu SET feature_code = 'CREDIT_NOTE_CREATE' WHERE url = '/credit-note';
-- ... etc
```

---

### Problema 2: Frontend sigue usando mapeo URL

**Síntoma:**

```javascript
// En logs:
[PermissionService] Validando feature: dashboard.view  ❌ No existe
// En lugar de:
[PermissionService] Validando feature: INVOICE_CREATE  ✅ Correcto
```

**Causa:**
Backend no está devolviendo `featureCode`, entonces el frontend usa el fallback del mapeo URL.

**Solución:**

1. Verificar que backend devuelve `featureCode` (Problema 1)
2. Si backend lo devuelve pero frontend no lo usa:

```typescript
// En menu-filter.service.ts, agregar log temporal:
private filterMenu(menu: Menu): Observable<Menu | null> {
  console.log('🔍 [DEBUG] Menu:', menu.name, 'featureCode:', menu.featureCode);

  const featureCode = menu.featureCode || this.getFeatureCodeFromUrl(menu.url);
  console.log('🔍 [DEBUG] Feature code final:', featureCode);

  // ... resto del código
}
```

---

### Problema 3: Menús siguen vacíos

**Síntoma:**

```javascript
📋 [MenuFilter] Menús recibidos del backend: 1 grupos
0: {id: 3, name: "Compañía", subMenus: []}  // ❌ Vacío
```

**Causa:**
Backend filtra por rol ANTES de enviar (no envía todos los menús).

**Solución:**
Modificar backend para que envíe TODOS los menús, sin filtrar por rol.

```java
// En MenuServiceImpl.java o MenuController.java
public List<MenuDto> getMenusByUsername(String username) {
    // ❌ ANTES: filtraba por rol
    // return menuRepository.findByUserRoles(user.getRoles());

    // ✅ AHORA: envía todos
    return menuRepository.findAllActive();
}
```

---

### Problema 4: Filtrado no funciona en Frontend

**Síntoma:**
Todos los menús se muestran, sin importar el plan.

**Verificar:**

1. Backend devuelve `featureCode` ✅
2. Frontend recibe el campo ✅
3. Llamadas a `/api/permission/validate-feature` se hacen ✅
4. Respuestas son correctas ✅

**Causa posible:**
El observable no se completa correctamente.

**Solución:**

```typescript
// En menu-filter.service.ts
// Asegurar que forkJoin espera a todas las observables
return forkJoin(filteredMenus$).pipe(
  map((filteredMenus) => {
    console.log("🔍 [DEBUG] forkJoin completado:", filteredMenus);
    return filteredMenus.filter((menu) => menu !== null) as Menu[];
  }),
  catchError((error) => {
    console.error("❌ Error en forkJoin:", error);
    return of([]); // Retornar array vacío en caso de error
  })
);
```

---

## 📊 Comparación: ANTES vs AHORA

### ANTES: Hardcoded en Frontend

```typescript
// Frontend (menu-filter.service.ts)
private readonly urlToFeatureMap = {
  '/billing': 'INVOICE_CREATE',  // 👈 Hardcoded
  '/credit-note': 'CREDIT_NOTE_CREATE',
  // Para agregar menú nuevo:
  '/nueva-ruta': 'NUEVO_FEATURE',  // 👈 Requiere deploy
};
```

**Para agregar menú:**

1. SQL: INSERT INTO menu (...)
2. SQL: INSERT INTO features (...)
3. **Frontend: Actualizar urlToFeatureMap** ← Deploy requerido
4. **Deploy frontend** ← Deploy requerido

---

### AHORA: Backend Envía Feature Code

```json
// Backend response
{
  "name": "Facturas",
  "url": "/billing",
  "featureCode": "INVOICE_CREATE" // 👈 Backend lo envía
}
```

```typescript
// Frontend (menu-filter.service.ts)
const featureCode = menu.featureCode; // ✅ Toma del backend
```

**Para agregar menú:**

1. SQL: INSERT INTO menu (..., feature_code = 'NUEVO_FEATURE')
2. SQL: INSERT INTO features (...)
3. ~~Frontend: Actualizar código~~ ❌ NO necesario
4. ~~Deploy frontend~~ ❌ NO necesario

**¡Solo 2 pasos vs 4 pasos!** 🎉

---

## 🎯 Testing Casos de Uso

### Caso 1: Usuario Plan FREE

**Setup:**

```sql
-- Usuario con plan FREE
SELECT u.identification, r.code_role, s.plan
FROM "user" u
JOIN user_role ur ON u.id = ur.user_id
JOIN role r ON ur.role_id = r.id
JOIN subscription s ON u.company_id = s.company_id
WHERE u.identification = '0102030405';

-- Resultado esperado:
-- identification | code_role | plan
-- 0102030405    | admin     | FREE
```

**Test:**

1. Login con usuario FREE
2. Ver menú lateral
3. Verificar en Console:

```javascript
📋 [MenuFilter] Menús recibidos del backend: 8 grupos
[PermissionService] Validando feature: INVOICE_CREATE → ✅ true
[PermissionService] Validando feature: CLIENT_CREATE → ✅ true
[PermissionService] Validando feature: CREDIT_NOTE_CREATE → ❌ false (no en plan)
[PermissionService] Validando feature: USER_MANAGEMENT → ❌ false (no en plan)
✅ [MenuFilter] Menús después de filtrar: 5 grupos
```

**Menú visible:**

- ✅ Facturas
- ✅ Clientes
- ✅ Productos
- ✅ Compañías
- ✅ Reportes (solo operativos)

**Menú oculto:**

- ❌ Notas de Crédito
- ❌ Usuarios
- ❌ Establecimientos
- ❌ Reportes Avanzados

---

### Caso 2: Usuario Plan PROFESSIONAL

**Setup:**

```sql
UPDATE subscription
SET plan = 'PROFESSIONAL'
WHERE company_id = (
  SELECT company_id FROM "user" WHERE identification = '0102030405'
);
```

**Test:**

1. Logout + Login (o recargar)
2. Ver menú lateral
3. Verificar en Console:

```javascript
📋 [MenuFilter] Menús recibidos del backend: 8 grupos
[PermissionService] Validando feature: INVOICE_CREATE → ✅ true
[PermissionService] Validando feature: CREDIT_NOTE_CREATE → ✅ true (en plan)
[PermissionService] Validando feature: USER_MANAGEMENT → ✅ true (en plan)
[PermissionService] Validando feature: REPORT_ADVANCED → ✅ true (en plan)
✅ [MenuFilter] Menús después de filtrar: 8 grupos (todos)
```

**Menú visible:**

- ✅ TODO visible (plan PRO tiene acceso completo)

---

### Caso 3: Agregar Menú Nuevo (Sin Deploy)

**Test de escalabilidad:**

```sql
-- 1. Agregar feature nuevo (si no existe)
INSERT INTO feature_permission (
  feature_code, feature_name, description, feature_module,
  allowed_roles, required_plans, is_active
) VALUES (
  'INVENTORY_MANAGEMENT',
  'Gestión de Inventario',
  'Control de stock y almacenes',
  'INVENTORY',
  ARRAY[1, 2],  -- admin, accountant
  ARRAY['PROFESSIONAL', 'ENTERPRISE'],
  true
);

-- 2. Agregar menú nuevo con feature_code
INSERT INTO menu (
  name, icon, url, feature_code, parent_id, active, order_num
) VALUES (
  'Inventario',
  'inventory',
  '/inventory',
  'INVENTORY_MANAGEMENT',  -- 👈 Feature code incluido
  NULL,  -- Es un grupo padre
  true,
  9
);

-- 3. Agregar submenú
INSERT INTO menu (
  name, icon, url, feature_code, parent_id, active, order_num
) VALUES (
  'Control de Stock',
  'warehouse',
  '/inventory/stock',
  'INVENTORY_MANAGEMENT',
  (SELECT id FROM menu WHERE url = '/inventory'),
  true,
  1
);

-- 4. ¡Listo! Recargar frontend (Cmd+R)
-- El nuevo menú aparece automáticamente (si el usuario tiene el plan correcto)
```

**Verificar:**

1. Recargar página (Cmd+R)
2. Ver Console:

```javascript
📋 [MenuFilter] Menús recibidos del backend: 9 grupos  // 👈 +1 nuevo
[PermissionService] Validando feature: INVENTORY_MANAGEMENT → ✅/❌ según plan
✅ [MenuFilter] Menús después de filtrar: X grupos
```

3. Si tienes plan PRO, debe aparecer "Inventario" en el menú
4. Si tienes plan FREE, NO aparece

**¡No se necesitó deploy del frontend!** 🎉

---

## ✅ Checklist Final

### Pre-Testing

- [ ] Backend ejecutándose (puerto 8280)
- [ ] Frontend ejecutándose (puerto 4200)
- [ ] Base de datos accesible
- [ ] Token JWT válido

### Backend

- [ ] Migración Flyway ejecutada sin errores
- [ ] Script de validación SQL ejecutado sin errores
- [ ] Endpoint `/api/menu/{id}` devuelve `featureCode`
- [ ] Todos los menús activos tienen `feature_code` poblado

### Frontend

- [ ] Logs `[MenuFilter]` aparecen en Console
- [ ] Se ven requests a `/api/permission/validate-feature`
- [ ] Menús se filtran según plan del usuario
- [ ] No hay errores en Console

### Funcional

- [ ] Usuario FREE ve solo menús básicos
- [ ] Usuario BASIC ve menús intermedios
- [ ] Usuario PRO ve todos los menús
- [ ] Agregar menú nuevo funciona sin deploy

---

## 🎉 Beneficios Obtenidos

✅ **Zero deploys** para agregar menús
✅ **Centralizado** en base de datos
✅ **Escalable** sin programar
✅ **Seguro** con validación backend
✅ **Flexible** para cambios rápidos
✅ **Mantenible** con FK constraints

---

## 📞 Soporte

Si algo no funciona:

1. **Pega los logs de Console** (Frontend)
2. **Pega el response del endpoint** `/api/menu/{id}`
3. **Pega el resultado del script de validación SQL**
4. **Indica el plan del usuario** de prueba

---

**Estado Global**: ✅ **IMPLEMENTACIÓN COMPLETA**
**Siguiente**: Testing y validación end-to-end
