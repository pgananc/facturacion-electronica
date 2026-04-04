# 📋 Resumen: Features Administrativos con Menús

## ✅ Implementación Completada

Se han creado 2 features administrativos con sus respectivos menús en "Administración Global" (solo para super_admin).

---

## 🎯 Features Creados

### 1. ROLE_PERMISSION_MANAGEMENT
- **Feature Code**: `ROLE_PERMISSION_MANAGEMENT`
- **Nombre**: Gestión de Permisos de Roles
- **Allowed Roles**: `[1]` (Solo Super Admin)
- **Required Plans**: Todos (FREE, BASIC, PROFESSIONAL, ENTERPRISE)
- **Endpoints**: En `ebill-authentication`
  - GET `/api/permission/permissions/features`
  - GET `/api/permission/permissions/role/{idRole}`
  - PUT `/api/permission/permissions/feature/{featureCode}`
  - PUT `/api/permission/permissions/bulk`

### 2. SUBSCRIPTION_MANAGEMENT
- **Feature Code**: `SUBSCRIPTION_MANAGEMENT`
- **Nombre**: Gestión de Suscripciones
- **Allowed Roles**: `[1]` (Solo Super Admin)
- **Required Plans**: Todos (FREE, BASIC, PROFESSIONAL, ENTERPRISE)
- **Endpoints**: En `ebill-billing`
  - GET `/api/subscriptions`
  - GET `/api/subscriptions/company/{idCompany}`
  - POST `/api/subscriptions/company/{idCompany}/upgrade`
  - POST `/api/subscriptions/company/{idCompany}/extend`
  - DELETE `/api/subscriptions/company/{idCompany}/cancel`

---

## 📂 Menús Creados

| ID | Nombre | URL | Feature Code | Icono | Parent |
|----|--------|-----|--------------|-------|--------|
| 32 | Gestión de Permisos | `/permissions` | `ROLE_PERMISSION_MANAGEMENT` | `admin_panel_settings` | 16 (Adm. Global) |
| 33 | Gestión de Suscripciones | `/subscriptions` | `SUBSCRIPTION_MANAGEMENT` | `card_membership` | 16 (Adm. Global) |

---

## 🗂️ Migraciones Flyway Creadas

### V2025121704 - Feature ROLE_PERMISSION_MANAGEMENT
```sql
-- Crea el feature en feature_permission
INSERT INTO feature_permission VALUES (
  'ROLE_PERMISSION_MANAGEMENT',
  'Gestión de Permisos de Roles',
  ...
  ARRAY[1], -- Solo super_admin
  ...
);
```

### V2025121705 - Feature SUBSCRIPTION_MANAGEMENT
```sql
-- Crea el feature en feature_permission
INSERT INTO feature_permission VALUES (
  'SUBSCRIPTION_MANAGEMENT',
  'Gestión de Suscripciones',
  ...
  ARRAY[1], -- Solo super_admin
  ...
);
```

### V2025121706 - Crear Menús
```sql
-- Menu ID 32: Gestión de Permisos
INSERT INTO menu VALUES (32, 16, 'admin_panel_settings', 'Gestión de Permisos', '/permissions', true, 'ROLE_PERMISSION_MANAGEMENT');

-- Menu ID 33: Gestión de Suscripciones
INSERT INTO menu VALUES (33, 16, 'card_membership', 'Gestión de Suscripciones', '/subscriptions', true, 'SUBSCRIPTION_MANAGEMENT');
```

### V2025121707 - Asignar Menús a Super Admin
```sql
-- Sincroniza secuencia
DO $$ ... setval('menu_role_seq', max_id) ... $$;

-- Asigna menu 32 al rol 1
INSERT INTO menu_role (id_menu, id_role) VALUES (32, 1);

-- Asigna menu 33 al rol 1
INSERT INTO menu_role (id_menu, id_role) VALUES (33, 1);
```

---

## 🎨 Estructura del Menú (Super Admin)

```
📁 Administración Global (ID: 16)
  ├── 📄 Gestionar Sucursales/Establecimientos (ID: 17)
  ├── 📄 Gestionar Puntos de Emisión (ID: 18)
  ├── 🆕 Gestión de Permisos (ID: 32) 👈 NUEVO
  └── 🆕 Gestión de Suscripciones (ID: 33) 👈 NUEVO
```

---

## 🚀 Testing

### 1. Verificar que los features se crearon

```sql
SELECT feature_code, feature_name, allowed_roles, module
FROM feature_permission
WHERE feature_code IN ('ROLE_PERMISSION_MANAGEMENT', 'SUBSCRIPTION_MANAGEMENT');
```

**Esperado**: 2 filas
- `ROLE_PERMISSION_MANAGEMENT` con `allowed_roles = {1}`
- `SUBSCRIPTION_MANAGEMENT` con `allowed_roles = {1}`

---

### 2. Verificar que los menús se crearon

```sql
SELECT id_menu, parent_id_menu, name, url, feature_code
FROM menu
WHERE id_menu IN (32, 33);
```

**Esperado**: 2 filas
- ID 32: Gestión de Permisos, `/permissions`, feature_code `ROLE_PERMISSION_MANAGEMENT`
- ID 33: Gestión de Suscripciones, `/subscriptions`, feature_code `SUBSCRIPTION_MANAGEMENT`

---

### 3. Verificar asignación al super_admin

```sql
SELECT mr.id_menu, m.name, mr.id_role
FROM menu_role mr
INNER JOIN menu m ON m.id_menu = mr.id_menu
WHERE mr.id_role = 1 
  AND mr.id_menu IN (32, 33)
  AND mr.status = true;
```

**Esperado**: 2 filas mostrando ambos menús asignados al rol 1

---

### 4. Contar menús totales del super_admin

```sql
SELECT COUNT(*) as total_menus
FROM menu_role
WHERE id_role = 1 AND status = true;
```

**Esperado**: ~33 menús (31 anteriores + 2 nuevos)

---

### 5. Probar endpoint de menú

```bash
# Obtener menú del super admin (usar username del super admin)
curl -X GET http://localhost:8280/api/menu/1722633961 \
  -H "Authorization: Bearer <token>"
```

**Verificar en response**:
```json
{
  "id": 16,
  "name": "Administración Global",
  "subMenus": [
    {
      "id": 17,
      "name": "Gestionar Sucursales/Establecimientos",
      ...
    },
    {
      "id": 32,
      "name": "Gestión de Permisos",
      "url": "/permissions",
      "featureCode": "ROLE_PERMISSION_MANAGEMENT"
    },
    {
      "id": 33,
      "name": "Gestión de Suscripciones",
      "url": "/subscriptions",
      "featureCode": "SUBSCRIPTION_MANAGEMENT"
    }
  ]
}
```

---

## 📊 Comparación de Roles

### Super Admin (Rol 1) - ANTES
- 31 menús
- Incluye: Administración Global

### Super Admin (Rol 1) - AHORA
- **33 menús** (+2 nuevos)
- Incluye: Administración Global
  - ✅ Gestión de Permisos (NUEVO)
  - ✅ Gestión de Suscripciones (NUEVO)

### Admin (Rol 3) - Sin cambios
- 28 menús
- **NO incluye** Administración Global
- **NO verá** los nuevos menús

---

## 🔒 Validación de Acceso

### Feature Validation (Frontend)

Cuando el frontend intente acceder a `/permissions`:

```bash
curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Content-Type: application/json" \
  -d '{
    "featureCode": "ROLE_PERMISSION_MANAGEMENT",
    "idCompany": 1
  }'
```

**Para Super Admin**: ✅ `{ "allowed": true }`
**Para Admin (rol 3)**: ❌ `{ "allowed": false, "reason": "ROLE_NOT_ALLOWED" }`

---

## 📁 Archivos Creados

```
✅ Migraciones Flyway (4 archivos):
   V2025121704__add_role_permission_management_feature.sql
   V2025121705__add_subscription_management_feature.sql
   V2025121706__create_admin_feature_menus.sql
   V2025121707__assign_admin_feature_menus_to_super_admin.sql

✅ Documentación:
   ADMIN_FEATURES_WITH_MENUS.md
```

---

## 🎯 Orden de Ejecución (Automático)

Al reiniciar la aplicación, Flyway ejecutará en orden:

1. ✅ V2025121702 - Asigna menús al rol Admin
2. ✅ V2025121703 - Asigna features al rol Admin
3. ✅ **V2025121704** - Crea feature `ROLE_PERMISSION_MANAGEMENT`
4. ✅ **V2025121705** - Crea feature `SUBSCRIPTION_MANAGEMENT`
5. ✅ **V2025121706** - Crea menús 32 y 33
6. ✅ **V2025121707** - Asigna menús 32 y 33 al super_admin

---

## ✅ Checklist de Validación

- [ ] Aplicación reiniciada
- [ ] 4 migraciones ejecutadas correctamente (V2025121704-07)
- [ ] Feature `ROLE_PERMISSION_MANAGEMENT` existe en DB
- [ ] Feature `SUBSCRIPTION_MANAGEMENT` existe en DB
- [ ] Menu ID 32 existe con feature_code correcto
- [ ] Menu ID 33 existe con feature_code correcto
- [ ] Ambos menús asignados al rol 1 (super_admin)
- [ ] Super admin tiene ~33 menús en total
- [ ] Endpoint `/api/menu/{username}` retorna menús 32 y 33 para super_admin
- [ ] Admin (rol 3) NO ve los menús 32 y 33

---

## 🚀 Próximo Paso

**REINICIA LA APLICACIÓN** para ejecutar las 4 migraciones nuevas.

**Log esperado:**
```
INFO - Migrating to: 2025121704 - add role permission management feature
INFO - Migrating to: 2025121705 - add subscription management feature
INFO - Migrating to: 2025121706 - create admin feature menus
INFO - Migrating to: 2025121707 - assign admin feature menus to super admin
INFO - Successfully applied 4 migrations
```

---

**Fecha**: 2025-12-18  
**Features**: `ROLE_PERMISSION_MANAGEMENT`, `SUBSCRIPTION_MANAGEMENT`  
**Menús**: 32, 33  
**Status**: ✅ COMPLETO - Listo para testing
