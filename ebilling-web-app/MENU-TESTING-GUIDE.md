# 🧪 Guía de Testing: Sistema de Menús con Feature Code

## 🎯 Objetivo

Verificar que el sistema de menús con `feature_code` funciona end-to-end, desde el backend hasta el filtrado en frontend.

---

## 📋 Pre-Requisitos

### 1. Servicios Corriendo

```bash
# Backend (Authentication Service)
./mvnw spring-boot:run
# Debe estar en: http://localhost:8280

# Frontend (Angular)
npm run start:local
# Debe estar en: http://localhost:4200

# Base de datos
# PostgreSQL en puerto 5433
```

### 2. Usuario de Prueba

```sql
-- Verificar que existe usuario de prueba
SELECT
  u.identification,
  u.name,
  r.code_role,
  c.name as company,
  s.plan,
  s.status
FROM "user" u
JOIN user_role ur ON u.id = ur.id_user
JOIN role r ON ur.id_role = r.id
JOIN company c ON u.company_id = c.id
LEFT JOIN subscription s ON c.id = s.company_id
WHERE u.identification = '0102030405';

-- Si no existe, crear uno con plan FREE
```

---

## 🧪 Test Suite

### Test 1: Verificar Migración Backend ✅

**Objetivo:** Confirmar que el campo `feature_code` existe y está poblado.

**Pasos:**

```sql
-- 1.1. Verificar estructura
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'menu' AND column_name = 'feature_code';

-- Esperado:
-- column_name  | data_type      | is_nullable
-- feature_code | character varying | YES

-- 1.2. Verificar datos poblados
SELECT
  id,
  name,
  url,
  feature_code,
  CASE
    WHEN feature_code IS NOT NULL THEN '✅'
    ELSE '⚠️'
  END as status
FROM menu
WHERE active = true AND url IS NOT NULL AND url != 'null'
ORDER BY id;

-- Esperado: Todos con ✅
-- Mínimo 10 menús con feature_code poblado
```

**Criterio de éxito:**

- ✅ Columna `feature_code` existe
- ✅ Al menos 10 menús tienen feature_code
- ✅ No hay menús importantes sin feature_code

---

### Test 2: Endpoint Backend Devuelve Feature Code ✅

**Objetivo:** Verificar que el endpoint `/api/menu/{id}` incluye `featureCode` en el response.

**Pasos:**

```bash
# 2.1. Obtener token (login)
TOKEN=$(curl -X POST http://localhost:8280/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identification":"0102030405","password":"admin"}' \
  | jq -r '.data.token')

echo "Token: $TOKEN"

# 2.2. Obtener menús
curl -X GET "http://localhost:8280/api/menu/0102030405" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.'

# 2.3. Verificar featureCode en respuesta
curl -X GET "http://localhost:8280/api/menu/0102030405" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.[].subMenus[] | {name, url, featureCode}'
```

**Response esperado:**

```json
{
  "name": "Facturas",
  "url": "/billing",
  "featureCode": "INVOICE_CREATE"  // 👈 Debe existir
}
{
  "name": "Nota de crédito",
  "url": "/credit-note",
  "featureCode": "CREDIT_NOTE_CREATE"  // 👈 Debe existir
}
```

**Criterio de éxito:**

- ✅ Response incluye campo `featureCode`
- ✅ `featureCode` NO es null para menús importantes
- ✅ `featureCode` coincide con los de la tabla `feature_permission`

---

### Test 3: Frontend Recibe Feature Code ✅

**Objetivo:** Confirmar que el frontend recibe el campo desde el backend.

**Pasos:**

1. Abrir aplicación: http://localhost:4200
2. Login con usuario: `0102030405` / `admin`
3. Abrir DevTools → Console (Cmd+Option+I)
4. Buscar logs:

```javascript
📋 [MenuFilter] Menús recibidos del backend: X grupos
📋 [MenuFilter] Estructura: (X) [{…}]
```

5. Expandir el objeto `[{…}]`
6. Verificar que los submenús tienen `featureCode`

**Console esperado:**

```javascript
📋 [MenuFilter] Estructura: (7) [{…}]
  0: {id: 6, name: "Facturas", subMenus: Array(1)}
    subMenus: Array(1)
      0: {id: 8, name: "Facturas", url: "/billing", featureCode: "INVOICE_CREATE"}  // 👈
```

**Criterio de éxito:**

- ✅ Log `[MenuFilter] Estructura` aparece
- ✅ Objetos de menú incluyen `featureCode`
- ✅ `featureCode` NO es undefined

---

### Test 4: Frontend Valida con Backend ✅

**Objetivo:** Verificar que el frontend llama a `/api/permission/validate-feature` usando el `featureCode`.

**Pasos:**

1. Estar logueado
2. DevTools → Network Tab
3. Filtrar por: `validate-feature`
4. Recargar página (Cmd+R)
5. Ver las peticiones POST a `/api/permission/validate-feature`

**Network Tab esperado:**

```
POST /api/permission/validate-feature
Request Payload:
{
  "featureCode": "INVOICE_CREATE",
  "idCompany": 1
}

Response:
{
  "allowed": true,
  "reason": "ACCESS_GRANTED",
  "message": "..."
}
```

**Criterio de éxito:**

- ✅ Se hacen múltiples requests a `validate-feature`
- ✅ El `featureCode` en el request es correcto (no es `dashboard.view`)
- ✅ Responses son 200 OK
- ✅ `allowed` es true/false según el plan

---

### Test 5: Filtrado Funciona para Plan FREE ✅

**Objetivo:** Usuario con plan FREE ve solo menús básicos.

**Setup:**

```sql
-- Asegurar que el usuario tiene plan FREE
UPDATE subscription
SET plan = 'FREE', status = 'ACTIVE'
WHERE company_id = (
  SELECT company_id FROM "user" WHERE identification = '0102030405'
);
```

**Pasos:**

1. Logout (si ya estaba logueado)
2. Login con `0102030405` / `admin`
3. Ver menú lateral
4. Ver Console logs

**Console esperado:**

```javascript
📋 [MenuFilter] Menús recibidos del backend: 8 grupos
[PermissionService] Validando feature: INVOICE_CREATE → ✅ true
[PermissionService] Validando feature: CLIENT_CREATE → ✅ true
[PermissionService] Validando feature: CREDIT_NOTE_CREATE → ❌ false
[PermissionService] Validando feature: USER_MANAGEMENT → ❌ false
[PermissionService] Validando feature: REPORT_ADVANCED → ❌ false
✅ [MenuFilter] Menús después de filtrar: 5 grupos
```

**Menú lateral esperado:**

```
✅ Visible:
- Facturas
- Clientes
- Productos
- Compañías
- Reportes (solo operativos: ventas por período, etc.)

❌ NO visible:
- Notas de Crédito
- Usuarios
- Establecimientos
- Reportes Avanzados (dashboard, comparativo)
```

**Criterio de éxito:**

- ✅ Se reciben todos los menús del backend (8 grupos)
- ✅ Se filtran correctamente (quedan 5 grupos)
- ✅ Items premium NO aparecen en el menú visual

---

### Test 6: Filtrado Funciona para Plan PROFESSIONAL ✅

**Objetivo:** Usuario con plan PRO ve todos los menús.

**Setup:**

```sql
UPDATE subscription
SET plan = 'PROFESSIONAL', status = 'ACTIVE'
WHERE company_id = (
  SELECT company_id FROM "user" WHERE identification = '0102030405'
);
```

**Pasos:**

1. Logout
2. Login con `0102030405` / `admin`
3. Ver menú lateral
4. Ver Console logs

**Console esperado:**

```javascript
📋 [MenuFilter] Menús recibidos del backend: 8 grupos
[PermissionService] Validando feature: INVOICE_CREATE → ✅ true
[PermissionService] Validando feature: CREDIT_NOTE_CREATE → ✅ true
[PermissionService] Validando feature: USER_MANAGEMENT → ✅ true
[PermissionService] Validando feature: REPORT_ADVANCED → ✅ true
✅ [MenuFilter] Menús después de filtrar: 8 grupos (todos)
```

**Menú lateral esperado:**

```
✅ Visible (TODO):
- Facturas
- Notas de Crédito
- Clientes
- Productos
- Usuarios
- Establecimientos
- Compañías
- Reportes (todos: operativos + avanzados)
```

**Criterio de éxito:**

- ✅ Se reciben todos los menús (8 grupos)
- ✅ NO se filtra nada (quedan 8 grupos)
- ✅ TODOS los items aparecen en el menú visual

---

### Test 7: Agregar Menú Sin Deploy Frontend ✅

**Objetivo:** Verificar que se puede agregar un menú nuevo solo con SQL, sin tocar código.

**Pasos:**

```sql
-- 1. Crear feature nuevo
INSERT INTO feature_permission (
  feature_code, feature_name, description, feature_module,
  allowed_roles, required_plans, is_active, created_at, updated_at
) VALUES (
  'TEST_FEATURE',
  'Feature de Prueba',
  'Feature para testing de menús dinámicos',
  'TESTING',
  ARRAY[1, 2, 3],  -- Todos los roles
  ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'],  -- Todos los planes
  true,
  NOW(),
  NOW()
);

-- 2. Crear menú nuevo
INSERT INTO menu (
  name, icon, url, feature_code, parent_id,
  active, order_num, created_at, updated_at
) VALUES (
  'Menú de Test',
  'science',
  '/test-menu',
  'TEST_FEATURE',
  NULL,  -- Grupo principal
  true,
  99,
  NOW(),
  NOW()
);

-- 3. Verificar que se creó
SELECT id, name, url, feature_code
FROM menu
WHERE name = 'Menú de Test';
```

**Pasos en Frontend:**

1. **NO hacer deploy**
2. Solo recargar página (Cmd+R)
3. Ver menú lateral

**Resultado esperado:**

```
✅ Aparece nuevo menú "Menú de Test" con ícono 🧪
✅ Al hacer click, intenta navegar a /test-menu
✅ En Console:
   [PermissionService] Validando feature: TEST_FEATURE → ✅ true
```

**Limpiar después del test:**

```sql
-- Eliminar menú de prueba
DELETE FROM menu WHERE name = 'Menú de Test';
DELETE FROM feature_permission WHERE feature_code = 'TEST_FEATURE';
```

**Criterio de éxito:**

- ✅ Menú aparece sin deploy del frontend
- ✅ Feature code se valida correctamente
- ✅ Sistema es completamente dinámico

---

### Test 8: Integridad Referencial ✅

**Objetivo:** Verificar que el FK constraint previene errores.

**Pasos:**

```sql
-- Test 1: Intentar insertar menú con feature_code inválido
INSERT INTO menu (
  name, icon, url, feature_code, parent_id, active, order_num
) VALUES (
  'Menú Inválido',
  'error',
  '/invalid',
  'FEATURE_QUE_NO_EXISTE',  -- ❌ No existe en feature_permission
  NULL,
  true,
  100
);

-- Esperado: ERROR: insert or update on table "menu" violates foreign key constraint
-- ✅ El FK constraint previene el error
```

**Test 2: Intentar eliminar feature usado**

```sql
-- Intentar eliminar feature que está en uso
DELETE FROM feature_permission WHERE feature_code = 'INVOICE_CREATE';

-- Esperado: ERROR: update or delete on table "feature_permission" violates foreign key constraint
-- ✅ No se puede eliminar feature en uso
```

**Criterio de éxito:**

- ✅ No se pueden insertar menús con features inexistentes
- ✅ No se pueden eliminar features en uso
- ✅ Integridad referencial garantizada

---

## 📊 Matriz de Testing

| Test                             | FREE | BASIC | PRO | Estado       |
| -------------------------------- | ---- | ----- | --- | ------------ |
| 1. Migración Backend             | N/A  | N/A   | N/A | ⏳ Pendiente |
| 2. Endpoint devuelve featureCode | N/A  | N/A   | N/A | ⏳ Pendiente |
| 3. Frontend recibe featureCode   | ✅   | ✅    | ✅  | ⏳ Pendiente |
| 4. Frontend valida con backend   | ✅   | ✅    | ✅  | ⏳ Pendiente |
| 5. Filtrado plan FREE            | ✅   | N/A   | N/A | ⏳ Pendiente |
| 6. Filtrado plan PRO             | N/A  | N/A   | ✅  | ⏳ Pendiente |
| 7. Agregar menú sin deploy       | ✅   | ✅    | ✅  | ⏳ Pendiente |
| 8. Integridad referencial        | N/A  | N/A   | N/A | ⏳ Pendiente |

---

## 🐛 Problemas Comunes y Soluciones

### Error 1: `featureCode` es null en frontend

**Causa:** Backend no ejecutó la migración.

**Solución:**

```sql
-- Verificar migración
SELECT version, description, installed_on
FROM flyway_schema_history
WHERE version = '2025121701';

-- Si no existe, ejecutar manualmente
-- (código en V2025121701__add_feature_code_to_menu.sql)
```

---

### Error 2: Menús no se filtran

**Causa:** PermissionService no se está llamando.

**Solución:**

```typescript
// Agregar log en menu-filter.service.ts
console.log("🔍 Llamando PermissionService para:", featureCode);
this.permissionService.canAccess(featureCode).subscribe((allowed) => {
  console.log("🔍 Resultado:", featureCode, "→", allowed);
});
```

---

### Error 3: Todos los menús se muestran

**Causa:** Usuario es `super_admin` (bypass).

**Solución:**

```sql
-- Verificar rol
SELECT r.code_role
FROM "user" u
JOIN user_role ur ON u.id = ur.id_user
JOIN role r ON ur.id_role = r.id
WHERE u.identification = '0102030405';

-- Si es super_admin, usar otro usuario o temporalmente cambiar rol
```

---

## ✅ Checklist Final

Después de ejecutar todos los tests:

- [ ] Test 1: Migración backend ✅
- [ ] Test 2: Endpoint devuelve featureCode ✅
- [ ] Test 3: Frontend recibe featureCode ✅
- [ ] Test 4: Frontend valida con backend ✅
- [ ] Test 5: Filtrado plan FREE ✅
- [ ] Test 6: Filtrado plan PRO ✅
- [ ] Test 7: Agregar menú sin deploy ✅
- [ ] Test 8: Integridad referencial ✅

---

## 🎉 Resultado Esperado

Si todos los tests pasan:

✅ **Backend:** Devuelve `featureCode` en todos los menús
✅ **Frontend:** Filtra menús según plan del usuario
✅ **Dinámico:** Agregar menús sin deploy funciona
✅ **Seguro:** FK constraint previene errores
✅ **Escalable:** Sistema listo para crecer

---

## 📞 Siguiente Paso

Una vez que todos los tests pasen:

1. Marcar como ✅ en el checklist
2. Documentar cualquier issue encontrado
3. Hacer commit de los cambios
4. Notificar al equipo que el sistema está listo

**¿Listo para empezar el testing?** 🚀
