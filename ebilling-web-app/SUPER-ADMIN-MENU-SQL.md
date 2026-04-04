# Menús Super Admin - Base de Datos

## 📋 Descripción

Este documento contiene los scripts SQL para configurar los menús de administración global para usuarios con rol `super_admin` en el sistema de facturación electrónica.

## 🔐 Permisos

Los menús solo son visibles para usuarios con:

- **Rol:** `super_admin`
- **Permisos:** Gestión global de sucursales y puntos de emisión de TODAS las compañías

---

## 📊 SQL Script - Inserts de Menús

### **✅ MENÚS YA CREADOS EN LA BASE DE DATOS**

Los siguientes menús fueron creados por **Jonathan Guerrero**:

```
Administración Global (ID: 16)
├── Gestión Global de Sucursales (ID: 17) → /admin/establishments
└── Gestión Global de Puntos de Emisión (ID: 18) → /admin/invoice-points
```

### Script SQL Ejecutado:

```sql
-- ===================================================================
-- MENÚ PADRE: Administración Global (Super Admin)
-- ===================================================================
INSERT INTO public.menu (
  id_menu,
  parent_id_menu,
  name,
  url,
  icon,
  status
) VALUES (
  16,           -- ✅ ID del menú padre
  NULL,         -- No tiene padre (menú raíz)
  'Administración Global',
  NULL,         -- No tiene URL (es un contenedor)
  'admin_panel_settings',
  true
);

-- ===================================================================
-- SUB-MENÚ 1: Gestión Global de Sucursales
-- ===================================================================
INSERT INTO public.menu (
  id_menu,
  parent_id_menu,
  name,
  url,
  icon,
  status
) VALUES (
  17,                             -- ✅ ID del sub-menú
  16,                             -- Padre: Administración Global
  'Gestión Global de Sucursales',
  '/admin/establishments',        -- Ruta del frontend
  'location_city',
  true
);

-- ===================================================================
-- SUB-MENÚ 2: Gestión Global de Puntos de Emisión
-- ===================================================================
INSERT INTO public.menu (
  id_menu,
  parent_id_menu,
  name,
  url,
  icon,
  status
) VALUES (
  18,                               -- ✅ ID del sub-menú
  16,                               -- Padre: Administración Global
  'Gestión Global de Puntos de Emisión',
  '/admin/invoice-points',          -- Ruta del frontend
  'pin_drop',
  true
);
```

---

## 🔗 Asignar Menús al Rol `super_admin`

### **Prerequisito:**

Asegúrate de que el rol `super_admin` exista en la tabla `role`:

```sql
-- Verificar si existe el rol super_admin
SELECT * FROM public.role WHERE name = 'super_admin';

-- Si NO existe, crear el rol super_admin
INSERT INTO public.role (id_role, name, description, status)
VALUES (
  5,                    -- Ajusta el ID según tu BD
  'super_admin',
  'Super Administrador con acceso global al sistema',
  true
);
```

### **Script de Asignación:**

```sql
-- ===================================================================
-- ASIGNAR MENÚS AL ROL super_admin
-- ===================================================================
-- Menú Padre: Administración Global
INSERT INTO public.role_menu (id_role, id_menu)
VALUES (
  (SELECT id_role FROM public.role WHERE name = 'super_admin'),
  16  -- ✅ Administración Global
);

-- Sub-menú: Gestión Global de Sucursales
INSERT INTO public.role_menu (id_role, id_menu)
VALUES (
  (SELECT id_role FROM public.role WHERE name = 'super_admin'),
  17  -- ✅ Gestión Global de Sucursales
);

-- Sub-menú: Gestión Global de Puntos de Emisión
INSERT INTO public.role_menu (id_role, id_menu)
VALUES (
  (SELECT id_role FROM public.role WHERE name = 'super_admin'),
  18  -- ✅ Gestión Global de Puntos de Emisión
);
```

---

## ✅ Verificación

### **1. Verificar que los menús fueron creados:**

```sql
SELECT
  m.id_menu,
  m.parent_id_menu,
  m.name,
  m.url,
  m.icon,
  m.status,
  parent.name AS parent_name
FROM public.menu m
LEFT JOIN public.menu parent ON m.parent_id_menu = parent.id_menu
WHERE m.id_menu IN (16, 17, 18)
ORDER BY m.parent_id_menu NULLS FIRST, m.id_menu;
```

**Resultado esperado:**

```
id_menu | parent_id_menu |              name               |          url          |        icon        | status | parent_name
--------+----------------+---------------------------------+-----------------------+--------------------+--------+-------------
   16   |      NULL      | Administración Global           | NULL                  | admin_panel_settings| true   | NULL
   17   |       16       | Gestión Global de Sucursales    | /admin/establishments | location_city      | true   | Administración Global
   18   |       16       | Gestión Global de Puntos... | /admin/invoice-points | pin_drop           | true   | Administración Global
```

### **2. Verificar asignación al rol super_admin:**

```sql
SELECT
  r.id_role,
  r.name AS role_name,
  m.id_menu,
  m.name AS menu_name,
  m.url
FROM public.role_menu rm
INNER JOIN public.role r ON rm.id_role = r.id_role
INNER JOIN public.menu m ON rm.id_menu = m.id_menu
WHERE r.name = 'super_admin'
  AND m.id_menu IN (16, 17, 18)
ORDER BY m.id_menu;
```

**Resultado esperado:**

```
id_role | role_name   | id_menu |              menu_name              |          url
--------+-------------+---------+-------------------------------------+-----------------------
   5    | super_admin |   16    | Administración Global               | NULL
   5    | super_admin |   17    | Gestión Global de Sucursales        | /admin/establishments
   5    | super_admin |   18    | Gestión Global de Puntos de Emisión | /admin/invoice-points
```

### **3. Verificar usuarios con rol super_admin:**

```sql
SELECT
  u.id,
  u.username,
  u.email,
  u.first_name,
  u.last_name,
  r.name AS role_name
FROM public.user u
INNER JOIN public.user_role ur ON u.id = ur.user_id
INNER JOIN public.role r ON ur.role_id = r.id_role
WHERE r.name = 'super_admin';
```

---

## 🎨 Iconos de Material Icons

Los iconos usados para los menús son de **Material Icons**:

- `admin_panel_settings` - Administración Global
- `location_city` - Sucursales
- `pin_drop` - Puntos de Emisión

Puedes ver más iconos en: [Material Icons](https://fonts.google.com/icons)

---

## 🛠️ Rollback (en caso de error)

Si necesitas revertir los cambios:

```sql
-- 1. Eliminar asignaciones del rol
DELETE FROM public.role_menu
WHERE id_role = (SELECT id_role FROM public.role WHERE name = 'super_admin')
  AND id_menu IN (16, 17, 18);

-- 2. Eliminar menús
DELETE FROM public.menu WHERE id_menu IN (17, 18, 16);

-- 3. (Opcional) Eliminar rol super_admin si fue creado
-- DELETE FROM public.role WHERE name = 'super_admin';
```

---

## 📝 Notas Importantes

1. **IDs de Menú:** Los IDs `16`, `17`, `18` fueron asignados por **Jonathan Guerrero** en la BD.

2. **Orden de Ejecución:**

   - Primero crear el menú padre (ID 16)
   - Luego los sub-menús (ID 17, 18)
   - Finalmente asignar al rol `super_admin`

3. **Seguridad:** El `AdminGuard` en el frontend valida que el usuario tenga el rol `super_admin` antes de permitir el acceso.

4. **Rutas Frontend:**

   - `/admin/establishments` - Gestión Global de Sucursales
   - `/admin/invoice-points` - Gestión Global de Puntos de Emisión

5. **Consistencia:** Asegúrate de que las rutas en la BD (`url`) coincidan con las configuradas en `app-routing.module.ts`.

---

## 🚀 Próximos Pasos (Post-Deployment)

1. **Asignar el rol `super_admin` a usuarios:**

   ```sql
   INSERT INTO public.user_role (user_id, role_id)
   VALUES (
     1,  -- ID del usuario (ej: jguerrero)
     (SELECT id_role FROM public.role WHERE name = 'super_admin')
   );
   ```

2. **Probar el acceso:**

   - Login con un usuario `super_admin`
   - Verificar que aparezca el menú "Administración Global"
   - Acceder a "Gestión Global de Sucursales"
   - Acceder a "Gestión Global de Puntos de Emisión"

3. **Verificar permisos:**
   - Un usuario con rol `admin` NO debe ver estos menús
   - Solo `super_admin` debe tener acceso

---

## 📧 Contacto

Para dudas sobre la implementación frontend:

- Desarrollador: Jorge Guerrero
- Proyecto: ebilling-web-app
- Fecha: Noviembre 2025

Para dudas sobre la base de datos:

- Equipo Backend
- Módulo: ebill-authentication
