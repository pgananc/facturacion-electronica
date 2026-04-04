# ✅ Verificación del Menú "Gestión de Permisos"

## 📊 Datos del Menú en BD

```
id_menu: 32
parent_id_menu: 16 (Administración Global)
icon: security
name: Gestión de Permisos
url: /admin/role-permissions
status: true
feature_code: ROLE_PERMISSION_MANAGEMENT
```

## 🔍 Verificaciones Necesarias

### 1. Verificar que está asignado al rol super_admin

```sql
-- Verificar asignación al rol super_admin
SELECT 
  rm.id_role_menu,
  r.id_role,
  r.name AS role_name,
  m.id_menu,
  m.name AS menu_name,
  m.url AS menu_url,
  m.feature_code
FROM public.role_menu rm
INNER JOIN public.role r ON rm.id_role = r.id_role
INNER JOIN public.menu m ON rm.id_menu = m.id_menu
WHERE r.name = 'super_admin'
  AND m.id_menu = 32;
```

**Si NO retorna resultados**, ejecutar:

```sql
-- Asignar el menú al rol super_admin
INSERT INTO public.role_menu (
  id_role,
  id_menu
) VALUES (
  (SELECT id_role FROM public.role WHERE name = 'super_admin'),
  32  -- ID del menú "Gestión de Permisos"
);
```

### 2. Verificar que el backend envía el menú

**En el navegador (DevTools → Network):**
1. Buscar la petición: `GET /api/menu/{userName}`
2. Ver la respuesta JSON
3. Buscar el menú con `id: 32` o `url: "/admin/role-permissions"`

**O ejecutar en la consola del navegador:**

```javascript
// Ver qué menús está recibiendo el frontend
const menuService = angular.injector(['ng']).get('MenuService');
// O mejor, buscar en los logs:
// [MenuFilterService] Menús recibidos del backend
```

### 3. Verificar que el feature existe y está activo

```sql
-- Verificar el feature
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
- `is_active = true`
- `allowed_roles` debe incluir el ID del rol `super_admin` (normalmente `1`)

### 4. Verificar estructura completa del menú padre

```sql
-- Ver todos los menús de Administración Global
SELECT 
  m.id_menu,
  m.parent_id_menu,
  m.name,
  m.url,
  m.icon,
  m.status,
  m.feature_code,
  COUNT(rm.id_role_menu) as assigned_to_roles
FROM public.menu m
LEFT JOIN public.role_menu rm ON m.id_menu = rm.id_menu
WHERE m.parent_id_menu = 16 OR m.id_menu = 16
GROUP BY m.id_menu, m.parent_id_menu, m.name, m.url, m.icon, m.status, m.feature_code
ORDER BY m.parent_id_menu NULLS FIRST, m.id_menu;
```

---

## 🐛 Problema Más Probable

Basado en que el menú existe en la BD pero no aparece, el problema más probable es:

**El menú NO está asignado al rol `super_admin` en la tabla `role_menu`**

### Solución Rápida:

```sql
-- Asignar el menú al rol super_admin
INSERT INTO public.role_menu (
  id_role,
  id_menu
) 
SELECT 
  r.id_role,
  32  -- ID del menú "Gestión de Permisos"
FROM public.role r
WHERE r.name = 'super_admin'
  AND NOT EXISTS (
    SELECT 1 
    FROM public.role_menu rm 
    WHERE rm.id_role = r.id_role 
      AND rm.id_menu = 32
  );
```

---

## ✅ Verificación Final

Después de ejecutar el INSERT, deberías:

1. **Recargar la página** (Ctrl+F5)
2. **Cerrar sesión y volver a iniciar sesión**
3. **Verificar en la consola** que aparece el log:
   ```
   [MenuFilterService] Menús recibidos del backend: { menus: [..., { id: 32, name: "Gestión de Permisos", ... }] }
   ```
4. **Verificar que el menú aparece** en el menú lateral bajo "Administración Global"

---

**Nota:** El ID del menú es `32`, no `41` como sugería la documentación anterior. Ajusta los scripts según tu BD.

