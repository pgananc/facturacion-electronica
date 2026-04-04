# 🔍 Guía de Debugging - Menú No Carga

## 📋 Checklist de Verificación

### 1. ✅ Verificar que el menú existe en el backend

**Ejecutar en el backend (SQL):**

```sql
-- Verificar que el menú fue creado
SELECT
  id_menu,
  parent_id_menu,
  name,
  url,
  icon,
  status,
  feature_code
FROM public.menu
WHERE url = '/admin/role-permissions'
   OR name LIKE '%Permisos%';
```

**Resultado esperado:**

```
id_menu | parent_id_menu | name                | url                      | icon      | status | feature_code
--------|----------------|---------------------|--------------------------|-----------|--------|------------------
41      | 16             | Gestión de Permisos | /admin/role-permissions | security  | true   | ROLE_PERMISSION_MANAGEMENT
```

### 2. ✅ Verificar que está asignado al rol super_admin

**Ejecutar en el backend (SQL):**

```sql
-- Verificar asignación al rol
SELECT
  rm.id_role_menu,
  r.name AS role_name,
  r.id_role,
  m.name AS menu_name,
  m.url AS menu_url,
  m.feature_code
FROM public.role_menu rm
INNER JOIN public.role r ON rm.id_role = r.id_role
INNER JOIN public.menu m ON rm.id_menu = m.id_menu
WHERE r.name = 'super_admin'
  AND m.url = '/admin/role-permissions';
```

**Resultado esperado:**

```
id_role_menu | role_name  | id_role | menu_name          | menu_url              | feature_code
-------------|------------|---------|-------------------|-----------------------|------------------
XXX          | super_admin| 1       | Gestión de Permisos| /admin/role-permissions| ROLE_PERMISSION_MANAGEMENT
```

### 3. ✅ Verificar que el backend envía el menú

**Abrir la consola del navegador y buscar:**

```
[MenuFilterService] Menús recibidos del backend:
```

**Verificar que el menú aparece en la lista con:**

- `name: "Gestión de Permisos"`
- `url: "/admin/role-permissions"`
- `featureCode: "ROLE_PERMISSION_MANAGEMENT"`

### 4. ✅ Verificar logs de filtrado

**Buscar en la consola:**

```
[MenuFilterService] Filtrando menú: Gestión de Permisos
[MenuFilterService] Menú "Gestión de Permisos" (ROLE_PERMISSION_MANAGEMENT): PERMITIDO/DENEGADO
```

**Si aparece DENEGADO, buscar el log de warning:**

```
[MenuFilterService] Menú "Gestión de Permisos" DENEGADO por falta de permiso
```

### 5. ✅ Verificar que el usuario tiene el rol super_admin

**En la consola del navegador:**

```javascript
// Ejecutar en la consola
const token = localStorage.getItem("token");
if (token) {
  const payload = JSON.parse(atob(token.split(".")[1]));
  console.log("Roles del usuario:", payload.roles);
  console.log("Es super_admin:", payload.roles?.includes("super_admin"));
}
```

**Resultado esperado:**

```
Roles del usuario: ["super_admin"]
Es super_admin: true
```

### 6. ✅ Verificar que el feature existe en el backend

**Ejecutar en el backend (SQL):**

```sql
-- Verificar que el feature existe
SELECT
  feature_code,
  name,
  module,
  allowed_roles,
  required_plans,
  is_active
FROM public.feature_permission
WHERE feature_code = 'ROLE_PERMISSION_MANAGEMENT';
```

**Resultado esperado:**

```
feature_code            | name                      | module         | allowed_roles | required_plans | is_active
------------------------|---------------------------|----------------|---------------|----------------|----------
ROLE_PERMISSION_MANAGEMENT | Gestión de Permisos de Roles | ADMINISTRATION | {1}          | {...}         | true
```

---

## 🐛 Problemas Comunes y Soluciones

### Problema 1: El menú no aparece en el backend

**Síntomas:**

- No aparece en la consulta SQL del paso 1
- El backend no lo envía

**Solución:**

1. Ejecutar el script SQL de `MENU-PERMISOS-SQL.md`
2. Verificar que el ID del menú no esté duplicado
3. Verificar que `status = true`

### Problema 2: El menú no está asignado al rol

**Síntomas:**

- Aparece en la BD pero no en el menú del usuario
- La consulta del paso 2 no retorna resultados

**Solución:**

1. Ejecutar el INSERT en `role_menu` del script SQL
2. Verificar que el `id_role` de `super_admin` sea correcto
3. Verificar que el `id_menu` coincida

### Problema 3: El backend no envía el menú

**Síntomas:**

- No aparece en los logs del frontend
- El array de menús está vacío o no incluye este menú

**Solución:**

1. Verificar que el endpoint `/api/menu/{userName}` retorna todos los menús
2. Verificar que el backend no está filtrando por rol antes de enviar
3. Verificar logs del backend para ver qué está enviando

### Problema 4: El menú es filtrado por permisos

**Síntomas:**

- Aparece en los logs como "DENEGADO"
- El log muestra: `Menú "Gestión de Permisos" DENEGADO por falta de permiso`

**Solución:**

1. Verificar que el usuario tiene el rol `super_admin`
2. Verificar que `hasBypassRole()` retorna `true` para `super_admin`
3. Verificar que el feature `ROLE_PERMISSION_MANAGEMENT` existe y está activo
4. Verificar que el rol `super_admin` (id=1) está en `allowed_roles` del feature

### Problema 5: El feature code no coincide

**Síntomas:**

- El menú tiene un `featureCode` diferente en el backend
- El mapeo de URL no funciona

**Solución:**

1. Verificar que `feature_code` en la BD sea exactamente `ROLE_PERMISSION_MANAGEMENT`
2. Verificar que el mapeo en `menu-filter.service.ts` incluya `/admin/role-permissions`
3. Verificar que la URL en la BD sea exactamente `/admin/role-permissions` (sin espacios, mayúsculas correctas)

---

## 🔧 Comandos de Debugging Rápido

### En el navegador (Consola):

```javascript
// 1. Ver token y roles
const token = localStorage.getItem("token");
const payload = JSON.parse(atob(token.split(".")[1]));
console.log("Roles:", payload.roles);
console.log("Es super_admin:", payload.roles?.includes("super_admin"));

// 2. Ver menús recibidos del backend
// Buscar en los logs: [MenuFilterService] Menús recibidos del backend

// 3. Verificar permisos
// Buscar en los logs: [PermissionService] Validación de ROLE_PERMISSION_MANAGEMENT
```

### En el backend (SQL):

```sql
-- Ver todo el menú de Administración Global
SELECT
  m.id_menu,
  m.name,
  m.url,
  m.feature_code,
  m.status,
  COUNT(rm.id_role_menu) as assigned_to_roles
FROM public.menu m
LEFT JOIN public.role_menu rm ON m.id_menu = rm.id_menu
WHERE m.parent_id_menu = 16
GROUP BY m.id_menu, m.name, m.url, m.feature_code, m.status
ORDER BY m.id_menu;

-- Verificar permisos del feature
SELECT * FROM public.feature_permission
WHERE feature_code = 'ROLE_PERMISSION_MANAGEMENT';
```

---

## 📝 Logs Esperados (Si Todo Funciona)

```
[MenuFilterService] Filtrando X menús recibidos del backend
[MenuFilterService] Menús recibidos del backend: { totalMenus: X, menus: [...] }
[MenuFilterService] Usuario tiene rol con bypass (super_admin), mostrando todos los menús
```

**O si tiene suscripción activa:**

```
[MenuFilterService] Filtrando menú: Gestión de Permisos, URL: /admin/role-permissions, featureCode: ROLE_PERMISSION_MANAGEMENT
[PermissionService] Validación de ROLE_PERMISSION_MANAGEMENT para companyId X: { allowed: true }
[MenuFilterService] Menú "Gestión de Permisos" (ROLE_PERMISSION_MANAGEMENT): PERMITIDO
```

---

## ✅ Verificación Final

Si después de seguir todos los pasos el menú aún no aparece:

1. **Recargar la página** (Ctrl+F5 o Cmd+Shift+R)
2. **Cerrar sesión y volver a iniciar sesión**
3. **Verificar la consola del navegador** para errores
4. **Verificar la red** (DevTools → Network) para ver la respuesta del endpoint `/api/menu/{userName}`

---

**Última actualización**: Diciembre 2024
