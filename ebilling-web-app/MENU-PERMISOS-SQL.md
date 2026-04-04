# 📋 Script SQL - Menú de Gestión de Permisos

## 🎯 Objetivo

Agregar la entrada de menú "Gestión de Permisos" al menú de **Administración Global** para usuarios `super_admin`.

Esta entrada permite acceder a:

- **Tab "Permisos de Roles"**: Gestionar qué roles pueden acceder a cada funcionalidad
- **Tab "Permisos de Planes"**: Gestionar qué planes incluyen cada funcionalidad

---

## 📊 Estructura del Menú

```
Administración Global (ID: 16)
├── Gestión Global de Sucursales (ID: 17) → /admin/establishments
├── Gestión Global de Puntos de Emisión (ID: 18) → /admin/invoice-points
├── Gestión de Suscripciones (ID: 40) → /admin/subscriptions
└── Gestión de Permisos (ID: 41) → /admin/role-permissions ⭐ NUEVO
```

---

## 🔧 Script SQL

### Paso 1: Verificar IDs Disponibles

```sql
-- Verificar el último ID usado en la tabla menu
SELECT MAX(id_menu) as ultimo_id FROM public.menu;

-- Verificar que el menú padre "Administración Global" existe
SELECT id_menu, name, url, icon
FROM public.menu
WHERE id_menu = 16;
```

### Paso 2: Crear Menú "Gestión de Permisos"

```sql
-- ===================================================================
-- MENÚ: Gestión de Permisos (Super Admin)
-- ===================================================================
INSERT INTO public.menu (
  id_menu,
  parent_id_menu,
  name,
  url,
  icon,
  status,
  feature_code
) VALUES (
  41,                              -- ✅ ID del menú (ajustar si es necesario)
  16,                              -- Padre: Administración Global
  'Gestión de Permisos',          -- Nombre del menú
  '/admin/role-permissions',       -- Ruta del frontend
  'security',                      -- Icono Material Icons
  true,                            -- Activo
  'ROLE_PERMISSION_MANAGEMENT'    -- Feature code para validación de permisos
);
```

### Paso 3: Asignar al Rol super_admin

```sql
-- ===================================================================
-- ASIGNAR MENÚ AL ROL super_admin
-- ===================================================================
-- Verificar que el rol super_admin existe
SELECT id_role, name, description
FROM public.role
WHERE name = 'super_admin';

-- Asignar el menú al rol super_admin
INSERT INTO public.role_menu (
  id_role,
  id_menu
) VALUES (
  (SELECT id_role FROM public.role WHERE name = 'super_admin'),
  41  -- ✅ Gestión de Permisos
);
```

---

## ✅ Verificación

### Verificar que el menú fue creado:

```sql
SELECT
  m.id_menu,
  m.parent_id_menu,
  m.name,
  m.url,
  m.icon,
  m.status,
  m.feature_code,
  parent.name AS parent_name
FROM public.menu m
LEFT JOIN public.menu parent ON m.parent_id_menu = parent.id_menu
WHERE m.id_menu = 41;
```

### Verificar que está asignado al rol super_admin:

```sql
SELECT
  rm.id_role_menu,
  r.name AS role_name,
  m.name AS menu_name,
  m.url AS menu_url,
  m.feature_code
FROM public.role_menu rm
INNER JOIN public.role r ON rm.id_role = r.id_role
INNER JOIN public.menu m ON rm.id_menu = m.id_menu
WHERE r.name = 'super_admin'
  AND m.id_menu = 41;
```

### Ver todos los menús de Administración Global:

```sql
SELECT
  m.id_menu,
  m.name,
  m.url,
  m.icon,
  m.feature_code,
  m.status
FROM public.menu m
WHERE m.parent_id_menu = 16
ORDER BY m.id_menu;
```

---

## 🎨 Iconos Disponibles

El menú usa el icono `security` de Material Icons. Otros iconos sugeridos:

- `security` - Gestión de Permisos (recomendado)
- `admin_panel_settings` - Administración Global
- `lock` - Permisos/Seguridad
- `verified_user` - Roles y Permisos
- `settings_applications` - Configuración de Permisos

Puedes ver más iconos en: [Material Icons](https://fonts.google.com/icons)

---

## 🔄 Rollback (en caso de error)

Si necesitas eliminar el menú:

```sql
-- 1. Eliminar asignación del rol
DELETE FROM public.role_menu
WHERE id_role = (SELECT id_role FROM public.role WHERE name = 'super_admin')
  AND id_menu = 41;

-- 2. Eliminar menú
DELETE FROM public.menu WHERE id_menu = 41;
```

---

## 📝 Notas Importantes

1. **ID del Menú**: El ID `41` fue sugerido en la documentación anterior. Si ya existe, usar el siguiente disponible.

2. **Feature Code**: `ROLE_PERMISSION_MANAGEMENT` debe existir en la tabla `feature_permission` del backend.

3. **Ruta Frontend**: La ruta `/admin/role-permissions` debe coincidir con la configurada en `admin-routing.module.ts`.

4. **Permisos**: El `PermissionGuard` valida que el usuario tenga el feature `ROLE_PERMISSION_MANAGEMENT` antes de permitir el acceso.

5. **Tabs Internos**: Una vez dentro de `/admin/role-permissions`, el usuario verá dos tabs:
   - "Permisos de Roles"
   - "Permisos de Planes"

---

## 🚀 Próximos Pasos

1. **Ejecutar el script SQL** en el backend
2. **Verificar** que el menú aparece en el menú lateral para usuarios `super_admin`
3. **Probar** el acceso a `/admin/role-permissions`
4. **Verificar** que ambos tabs funcionan correctamente

---

**Última actualización**: Diciembre 2024
