# ✅ Gestión de Permisos de Roles - Implementación Completada

**Fecha:** 19 de Diciembre, 2025  
**Feature:** `ROLE_PERMISSION_MANAGEMENT`  
**Roles Permitidos:** super_admin (id=1)  
**Estado:** ✅ Frontend completado, esperando endpoints de backend

---

## 📦 Estructura Creada

### Modelos (`_model/permission/`)

- ✅ `permission-admin.ts` - DTOs para administración de permisos
  - `FeaturePermissionAdmin` - Información de un feature con sus permisos
  - `Role` - Información de un rol
  - `RolePermissionUpdate` - Request para actualizar permisos de un feature
  - `PermissionMatrixRow` - Fila de la matriz de permisos
  - `BulkPermissionUpdateRequest` - Request para actualizar múltiples permisos

### Servicios (`_service/permission/`)

- ✅ `permission-admin.service.ts` - Servicio HTTP para gestión de permisos
  - `getAllFeatures()` - Obtener todos los features
  - `getRolePermissions(roleId)` - Obtener permisos de un rol
  - `getAllRoles()` - Obtener todos los roles
  - `updateFeatureRoles(featureCode, allowedRoles)` - Actualizar roles permitidos
  - `syncRolePermissions(roleId, features)` - Sincronizar permisos de un rol
  - `bulkUpdatePermissions(request)` - Actualizar múltiples permisos en lote

### Componentes (`pages/admin/role-permissions/`)

#### ✅ PermissionMatrixComponent

**Archivo:** `permission-matrix/permission-matrix.component.ts`

**Características:**

- **Matriz Interactiva:** Tabla con features (filas) x roles (columnas)
- **Checkboxes:** Toggle de permisos para cada combinación feature-rol
- **Filtro por Módulo:** Dropdown para ver solo features de un módulo específico
- **Tracking de Cambios:** Map para rastrear cambios sin guardar
- **Guardado en Lote:** Envía todos los cambios en una sola petición
- **Indicador Visual:** Banner que muestra cuando hay cambios pendientes
- **Descartar Cambios:** Botón para revertir cambios sin guardar

**Columnas de la Tabla:**

- Funcionalidad (nombre + código)
- Módulo (badge con icono y color)
- Planes requeridos (chips)
- Columnas dinámicas por rol (checkboxes)

**Features de UX:**

- Sticky header (header se queda fijo al hacer scroll)
- Scroll horizontal para tablas anchas
- Colores distintivos por módulo
- Icons contextuales
- Empty state cuando no hay datos
- Loading state
- Leyenda explicativa

---

## 🎨 UI/UX Features

### Diseño Visual

**Colores por Módulo:**

- 🔵 INVOICING - Azul (`#2196f3`)
- 🟣 CREDIT_NOTES - Púrpura (`#9c27b0`)
- 🟢 CLIENTS - Verde (`#4caf50`)
- 🟠 REPORTS - Naranja (`#ff9800`)
- 🔴 ADMINISTRATION - Rojo (`#f44336`)
- ⚫ API - Gris (`#607d8b`)

**Icons por Módulo:**

- 📄 INVOICING → `receipt`
- 📄 CREDIT_NOTES → `receipt_long`
- 👥 CLIENTS → `people`
- 📊 REPORTS → `bar_chart`
- ⚙️ ADMINISTRATION → `settings`
- 🔌 API → `api`

**Chips de Planes:**

- 🔵 FREE
- 🟠 BASIC
- 🟢 PROFESSIONAL
- 🟣 ENTERPRISE

### Responsive Design

- Tabla con scroll horizontal en mobile
- Toolbar se apila verticalmente en pantallas pequeñas
- Botones ocupan ancho completo en mobile

---

## 🔌 Endpoints Esperados (Backend)

### Base URL

```
http://localhost:8280/api/permissions
```

### 1. Obtener Todos los Features

```
GET /api/permissions/features
```

**Response:**

```json
[
  {
    "featureCode": "INVOICE_CREATE",
    "name": "Crear facturas",
    "module": "INVOICING",
    "allowedRoles": [1, 2, 3],
    "requiredPlans": ["FREE", "BASIC", "PROFESSIONAL", "ENTERPRISE"],
    "description": "Permite crear facturas electrónicas"
  },
  {
    "featureCode": "REPORT_ADVANCED",
    "name": "Reportes avanzados",
    "module": "REPORTS",
    "allowedRoles": [1, 2],
    "requiredPlans": ["PROFESSIONAL", "ENTERPRISE"],
    "description": "Acceso a reportes avanzados y dashboard"
  }
]
```

### 2. Obtener Permisos de un Rol

```
GET /api/permissions/role/{roleId}
```

**Response:** Array de `FeaturePermissionAdmin` (solo features permitidos para ese rol)

### 3. Obtener Todos los Roles

```
GET /api/permissions/roles
```

**Response:**

```json
[
  {
    "id": 1,
    "name": "Super Admin",
    "description": "Acceso total al sistema"
  },
  {
    "id": 2,
    "name": "Member",
    "description": "Usuario básico"
  },
  {
    "id": 3,
    "name": "Admin",
    "description": "Administrador de empresa"
  },
  {
    "id": 4,
    "name": "Accountant",
    "description": "Contador"
  },
  {
    "id": 5,
    "name": "Operator",
    "description": "Operador"
  }
]
```

### 4. Actualizar Roles Permitidos (Individual)

```
PUT /api/permissions/feature/{featureCode}/roles
Content-Type: application/json

{
  "allowedRoles": [1, 2, 3]
}
```

**Response:** `204 No Content` o `200 OK`

### 5. Sincronizar Permisos de un Rol

```
POST /api/permissions/role/{roleId}/sync
Content-Type: application/json

{
  "features": ["INVOICE_CREATE", "CLIENT_CREATE", "REPORT_OPERATIONAL"]
}
```

**Response:** `204 No Content` o `200 OK`

### 6. Actualizar Múltiples Permisos en Lote (Recomendado)

```
POST /api/permissions/bulk-update
Content-Type: application/json

{
  "updates": [
    {
      "featureCode": "INVOICE_CREATE",
      "allowedRoles": [1, 2, 3, 4]
    },
    {
      "featureCode": "REPORT_ADVANCED",
      "allowedRoles": [1, 2]
    },
    {
      "featureCode": "CLIENT_CREATE",
      "allowedRoles": [1, 2, 3, 4, 5]
    }
  ]
}
```

**Response:** `204 No Content` o `200 OK`

---

## 🔒 Seguridad

### Guards Aplicados

- ✅ `SessionValidationGuard` - Valida sesión activa
- ✅ `PermissionGuard` - Verifica feature `ROLE_PERMISSION_MANAGEMENT`

### Validación Backend Requerida

El backend DEBE validar en cada endpoint:

1. ✅ Usuario autenticado
2. ✅ Usuario tiene rol super_admin (id=1)
3. ✅ Feature `ROLE_PERMISSION_MANAGEMENT` está activo

### Auditoría Recomendada

El backend DEBERÍA registrar:

- Quién hizo el cambio (idUser)
- Qué cambió (featureCode, allowedRoles anterior y nuevo)
- Cuándo se hizo (timestamp)
- Razón del cambio (opcional)

---

## 🧪 Testing

### Pruebas Manuales a Realizar

#### 1. Acceso a la Página

- [ ] Super_admin puede acceder a `/admin/role-permissions`
- [ ] Admin/Member/Accountant/Operator reciben 403

#### 2. Carga de Datos

- [ ] Tabla carga todos los features
- [ ] Columnas de roles se generan dinámicamente
- [ ] Checkboxes muestran estado correcto inicial
- [ ] Filtro de módulos funciona

#### 3. Modificar Permisos

- [ ] Click en checkbox cambia estado visual
- [ ] Indicador "cambios pendientes" aparece
- [ ] Contador de cambios es correcto
- [ ] Múltiples cambios se acumulan correctamente

#### 4. Guardar Cambios

- [ ] Botón "Guardar" envía request correcto
- [ ] Loading state se muestra
- [ ] Toast de éxito aparece
- [ ] Tabla se recarga con nuevos datos
- [ ] Indicador "cambios pendientes" desaparece

#### 5. Descartar Cambios

- [ ] Confirmación se muestra
- [ ] Cambios se revierten visualmente
- [ ] Tabla vuelve al estado original
- [ ] Indicador desaparece

#### 6. Filtros

- [ ] Filtro por módulo filtra correctamente
- [ ] "Todos los módulos" muestra todo
- [ ] Empty state se muestra cuando no hay datos

---

## 📊 Métricas de Código

- **Archivos TypeScript:** 3
- **Archivos HTML:** 1
- **Archivos CSS:** 1
- **Archivos de Módulos:** 2
- **Líneas de Código (aprox):** ~800
- **Tiempo de Implementación:** ~1.5 horas

---

## 🚀 Próximos Pasos

### Inmediato

1. ⏳ **Esperar endpoints de backend**
2. ⏳ **Feature code en BD:** `ROLE_PERMISSION_MANAGEMENT`
3. ⏳ **Entrada en menú:** Backend debe agregar a tabla `menu`

### Testing

1. ⏳ Probar con datos reales del backend
2. ⏳ Verificar guardado en lote funciona correctamente
3. ⏳ Validar que usuarios afectados ven cambios después de re-login

### Mejoras Futuras (Opcional)

- Búsqueda de features por nombre
- Exportar configuración de permisos a JSON
- Importar configuración desde JSON
- Historial de cambios de permisos
- Preview de qué usuarios se verán afectados
- Confirmación con lista de usuarios activos afectados

---

## 🎯 Flujo de Uso

### Ejemplo: Dar permisos de facturación a "Member"

1. Super_admin ingresa a `/admin/role-permissions`
2. Ve la matriz con todos los features y roles
3. Busca la fila "INVOICE_CREATE" (Crear facturas)
4. Marca el checkbox en la columna "Member"
5. Marca otros checkboxes necesarios
6. Ve el indicador: "Tienes 3 cambios sin guardar"
7. Click en "Guardar Cambios"
8. Backend actualiza `feature_permission.allowed_roles`
9. Toast: "3 permisos actualizados exitosamente"
10. Usuarios con rol "Member" deben cerrar sesión y volver a entrar
11. Ahora pueden ver opción "Nueva Factura" en su menú

---

## 📝 Notas para el Backend

### Feature Code

```sql
INSERT INTO feature_permission (feature_code, name, module, allowed_roles, required_plans)
VALUES ('ROLE_PERMISSION_MANAGEMENT', 'Gestión de Permisos de Roles', 'ADMINISTRATION',
        ARRAY[1], ARRAY['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE']);
```

### Entrada en Menú

```sql
INSERT INTO menu (id_menu, parent_id_menu, icon, name, url, status, feature_code)
VALUES (nextval('menu_seq'), 16, 'security', 'Gestión de Permisos',
        '/admin/role-permissions', true, 'ROLE_PERMISSION_MANAGEMENT');

INSERT INTO menu_role (id_menu_role, id_menu, id_role, status)
VALUES (nextval('menu_role_seq'), currval('menu_seq'), 1, true);
```

### Endpoint de Bulk Update (Recomendado)

```java
@PostMapping("/bulk-update")
public ResponseEntity<Void> bulkUpdatePermissions(@RequestBody BulkPermissionUpdateRequest request) {
    // Validar que usuario es super_admin
    if (!isSuperAdmin()) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // Actualizar cada feature
    for (RolePermissionUpdate update : request.getUpdates()) {
        featurePermissionRepository.updateAllowedRoles(
            update.getFeatureCode(),
            update.getAllowedRoles()
        );

        // Registrar en auditoría
        auditService.logPermissionChange(
            getCurrentUserId(),
            update.getFeatureCode(),
            update.getAllowedRoles()
        );
    }

    return ResponseEntity.noContent().build();
}
```

### SQL para Actualizar allowed_roles

```sql
UPDATE feature_permission
SET allowed_roles = ARRAY[1, 2, 3]::INTEGER[]
WHERE feature_code = 'INVOICE_CREATE';
```

---

## 💡 Arquitectura de la Matriz

```
┌────────────────────────────────────────────────────────────┐
│                     MATRIZ DE PERMISOS                     │
├────────────────┬─────────┬─────────┬─────────┬─────────────┤
│ Funcionalidad  │ Módulo  │ Planes  │ Rol 1   │ Rol 2 ...   │
├────────────────┼─────────┼─────────┼─────────┼─────────────┤
│ INVOICE_CREATE │INVOICING│ ALL     │ ☑       │ ☐           │
│ CLIENT_CREATE  │ CLIENTS │ ALL     │ ☑       │ ☑           │
│ REPORT_ADVANCED│ REPORTS │ PRO,ENT │ ☑       │ ☐           │
└────────────────┴─────────┴─────────┴─────────┴─────────────┘

Cambios Pendientes: 2
[Descartar] [Guardar Cambios]
```

---

## 🔄 Integración con Sistema de Permisos Existente

### PermissionService (Frontend)

El `PermissionService` existente ya hace:

```typescript
POST /api/permission/validate-feature
{
  "featureCode": "INVOICE_CREATE",
  "idCompany": 1
}
```

El backend consulta `feature_permission.allowed_roles` para validar.

**Flujo después de cambiar permisos:**

1. Super_admin cambia `INVOICE_CREATE.allowedRoles` a `[1, 2, 3, 4]`
2. Backend actualiza `feature_permission` table
3. Usuario con rol 4 (Accountant) cierra sesión y vuelve a entrar
4. Frontend llama `PermissionService.canAccess('INVOICE_CREATE')`
5. Backend valida: rol 4 está en `[1, 2, 3, 4]` → ✅ Permitido
6. Frontend muestra botón "Nueva Factura"

---

**Estado:** ✅ **Frontend completado. Esperando endpoints de backend para testing.**

**Ruta:** `/admin/role-permissions`  
**Protección:** `SessionValidationGuard` + `PermissionGuard` (ROLE_PERMISSION_MANAGEMENT)
