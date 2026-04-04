# 🔐 Feature: ROLE_PERMISSION_MANAGEMENT

## 📋 Descripción

Feature administrativo que permite a los **Super Admin** gestionar dinámicamente los permisos de los roles del sistema. Con este feature, se pueden asignar o remover features de cualquier rol sin necesidad de ejecutar migraciones SQL.

## 🎯 Características

- **Listar todos los features** con sus permisos actuales
- **Ver permisos de un rol específico** con todos sus features asignados
- **Actualizar roles permitidos** de un feature específico
- **Asignar múltiples features** a un rol de una sola vez

## 🔒 Restricciones de Acceso

| Propiedad | Valor |
|-----------|-------|
| **Feature Code** | `ROLE_PERMISSION_MANAGEMENT` |
| **Allowed Roles** | `[1]` (Solo Super Admin) |
| **Required Plans** | `['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE']` |
| **Is Active** | `true` |

## 📡 Endpoints API

### 1. GET /api/permissions/features

**Descripción**: Lista todos los features del sistema con sus detalles de permisos.

**Autenticación**: Requerida (Super Admin)

**Request**:
```bash
curl -X GET http://localhost:8280/api/permissions/features \
  -H "Authorization: Bearer <token>"
```

**Response 200 OK**:
```json
[
  {
    "featureCode": "INVOICE_CREATE",
    "featureName": "Crear Facturas",
    "description": "Crear y emitir facturas electrónicas",
    "module": "INVOICING",
    "allowedRoles": [1, 2, 3],
    "requiredPlans": ["FREE", "BASIC", "PROFESSIONAL", "ENTERPRISE"],
    "isActive": true
  },
  {
    "featureCode": "ROLE_PERMISSION_MANAGEMENT",
    "featureName": "Gestión de Permisos de Roles",
    "description": "Permite gestionar y asignar permisos (features) a diferentes roles",
    "module": "ADMINISTRATION",
    "allowedRoles": [1],
    "requiredPlans": ["FREE", "BASIC", "PROFESSIONAL", "ENTERPRISE"],
    "isActive": true
  }
]
```

---

### 2. GET /api/permissions/role/{idRole}

**Descripción**: Obtiene todos los permisos (features) asignados a un rol específico.

**Autenticación**: Requerida (Super Admin)

**Parámetros**:
- `idRole` (path): ID del rol a consultar

**Request**:
```bash
curl -X GET http://localhost:8280/api/permissions/role/3 \
  -H "Authorization: Bearer <token>"
```

**Response 200 OK**:
```json
{
  "roleId": 3,
  "roleName": "Admin",
  "totalFeatures": 14,
  "permissions": [
    {
      "featureCode": "INVOICE_CREATE",
      "featureName": "Crear Facturas",
      "description": "Crear y emitir facturas electrónicas",
      "module": "INVOICING",
      "allowedRoles": [1, 2, 3],
      "requiredPlans": ["FREE", "BASIC", "PROFESSIONAL", "ENTERPRISE"],
      "isActive": true
    },
    {
      "featureCode": "USER_MANAGEMENT",
      "featureName": "Gestión de Usuarios",
      "description": "Crear y administrar usuarios del sistema",
      "module": "ADMINISTRATION",
      "allowedRoles": [1, 2, 3],
      "requiredPlans": ["BASIC", "PROFESSIONAL", "ENTERPRISE"],
      "isActive": true
    }
    // ... más features
  ]
}
```

---

### 3. PUT /api/permissions/feature/{featureCode}

**Descripción**: Actualiza la lista de roles permitidos para un feature específico.

**Autenticación**: Requerida (Super Admin)

**Parámetros**:
- `featureCode` (path): Código del feature a actualizar

**Request**:
```bash
curl -X PUT http://localhost:8280/api/permissions/feature/INVOICE_EXPORT \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "allowedRoles": [1, 2, 3]
  }'
```

**Request Body**:
```json
{
  "allowedRoles": [1, 2, 3]
}
```

**Response 200 OK**:
```json
{
  "featureCode": "INVOICE_EXPORT",
  "featureName": "Exportar Facturas",
  "description": "Exportar facturas en diferentes formatos",
  "module": "INVOICING",
  "allowedRoles": [1, 2, 3],
  "requiredPlans": ["PROFESSIONAL", "ENTERPRISE"],
  "isActive": true
}
```

**⚠️ Nota Importante**: Este endpoint **reemplaza completamente** la lista de `allowedRoles`. Si quieres remover un rol, simplemente no lo incluyas en el array.

---

---

## 🎯 Casos de Uso

### Caso 1: Ver todos los features disponibles

```bash
# Listar todos los features para ver qué permisos existen
curl -X GET http://localhost:8280/api/permissions/features \
  -H "Authorization: Bearer <super_admin_token>"
```

**Uso**: Permite al super admin ver qué features existen en el sistema y cuáles roles tienen acceso a cada uno.

---

### Caso 2: Verificar permisos de un rol

```bash
# Ver qué features tiene asignados el rol Admin (id=3)
curl -X GET http://localhost:8280/api/permissions/role/3 \
  -H "Authorization: Bearer <super_admin_token>"
```

**Uso**: Auditar los permisos de un rol específico.

---

### Caso 3: Dar acceso a un feature a un nuevo rol

```bash
# Agregar rol Admin (3) a INVOICE_EXPORT
# Actualmente: allowedRoles = [1, 2]
# Después: allowedRoles = [1, 2, 3]

curl -X PUT http://localhost:8280/api/permissions/feature/INVOICE_EXPORT \
  -H "Authorization: Bearer <super_admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "allowedRoles": [1, 2, 3]
  }'
```

**Uso**: Otorgar permisos de un feature a un rol adicional.

---

### Caso 4: Remover acceso de un rol a un feature

```bash
# Remover rol Member (2) de INVOICE_VOID
# Actualmente: allowedRoles = [1, 2, 3]
# Después: allowedRoles = [1, 3]

curl -X PUT http://localhost:8280/api/permissions/feature/INVOICE_VOID \
  -H "Authorization: Bearer <super_admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "allowedRoles": [1, 3]
  }'
```

**Uso**: Revocar permisos de un feature a un rol específico.

---

### Caso 5: Asignar múltiples features a un nuevo rol

```bash
# Crear rol "Contador" (id=4) y asignarle múltiples features usando bulk update

curl -X PUT http://localhost:8280/api/permission/permissions/bulk \
  -H "Authorization: Bearer <super_admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "featureRoles": {
      "INVOICE_CREATE": [1, 2, 3, 4],
      "INVOICE_EXPORT": [1, 2, 3, 4],
      "CREDIT_NOTE_CREATE": [1, 2, 3, 4],
      "CLIENT_CREATE": [1, 2, 3, 4],
      "REPORT_OPERATIONAL": [1, 2, 3, 4],
      "REPORT_SALES": [1, 2, 3, 4]
    }
  }'
```

**Uso**: Configurar rápidamente múltiples features para un nuevo rol en una sola operación.

---

## 🔧 Implementación Técnica

### Entidades Involucradas

```java
// FeaturePermission entity
@Entity
public class FeaturePermission {
    private String featureCode;
    private String featureName;
    private String description;
    private String module;
    private Integer[] allowedRoles;  // Array de IDs de roles
    private String[] requiredPlans;
    private Boolean isActive;
}
```

### DTOs Creados

1. **`FeaturePermissionDto`**: Representa un feature con sus permisos
2. **`RolePermissionsDto`**: Agrupa todos los features de un rol
3. **`UpdateFeatureRolesDto`**: Para actualizar `allowedRoles`

### Métodos del Servicio

```java
public interface IFeaturePermissionService {
    // Métodos de gestión
    List<FeaturePermissionDto> getAllFeaturesForManagement();
    RolePermissionsDto getRolePermissions(Integer roleId);
    FeaturePermissionDto updateFeatureRoles(String featureCode, UpdateFeatureRolesDto dto);
    List<FeaturePermissionDto> bulkUpdateRoles(BulkUpdateRolesDto dto);
}
```

---

## 🧪 Testing

### Test 1: Listar features

```bash
curl -X GET http://localhost:8280/api/permissions/features
```

**Verificar**:
- ✅ Retorna array de features
- ✅ Cada feature tiene: featureCode, featureName, allowedRoles, etc.
- ✅ Status 200 OK

---

### Test 2: Ver permisos de Super Admin (rol 1)

```bash
curl -X GET http://localhost:8280/api/permissions/role/1
```

**Verificar**:
- ✅ Retorna todos los features (16 features)
- ✅ `roleName`: "Super Admin"
- ✅ `totalFeatures`: 16

---

### Test 3: Actualizar permisos de un feature

```bash
# Paso 1: Ver estado actual
curl -X GET http://localhost:8280/api/permissions/features | jq '.[] | select(.featureCode=="WEBHOOK_CONFIG")'

# Paso 2: Agregar rol 2
curl -X PUT http://localhost:8280/api/permissions/feature/WEBHOOK_CONFIG \
  -H "Content-Type: application/json" \
  -d '{"allowedRoles": [1, 2]}'

# Paso 3: Verificar cambio
curl -X GET http://localhost:8280/api/permissions/features | jq '.[] | select(.featureCode=="WEBHOOK_CONFIG")'
```

**Verificar**:
- ✅ `allowedRoles` se actualizó correctamente
- ✅ Status 200 OK

---

### Test 4: Actualización masiva de roles (bulk)

```bash
curl -X PUT http://localhost:8280/api/permission/permissions/bulk \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "featureRoles": {
      "API_ACCESS": [1, 2, 3],
      "WEBHOOK_CONFIG": [1, 2]
    }
  }'
```

**Verificar**:
- ✅ Múltiples features actualizados en una sola operación
- ✅ `allowedRoles` se actualizó correctamente para cada feature
- ✅ Status 200 OK

---

## 🔒 Seguridad

### Validaciones Implementadas

1. **Autenticación**: Todos los endpoints requieren token JWT válido
2. **Autorización**: Solo super_admin (rol 1) puede acceder
3. **Validación de datos**:
   - `allowedRoles` no puede estar vacío
   - `featureCodes` debe contener al menos un código
4. **Manejo de errores**:
   - 404 si feature no existe
   - 403 si usuario no tiene permisos
   - 400 si datos inválidos

### Recomendaciones

- ✅ **Auditar cambios**: Log cada modificación de permisos
- ✅ **Backup antes de cambios masivos**: Exportar estado actual
- ⚠️ **Cuidado con `allowedRoles`**: PUT reemplaza completamente
- ⚠️ **No remover super_admin de features críticos**

---

## 📊 Ejemplos de Configuraciones de Roles

### Super Admin (Rol 1)
```json
{
  "roleId": 1,
  "features": "ALL" // Tiene acceso a todos los features
}
```

### Admin (Rol 3)
```json
{
  "roleId": 3,
  "features": [
    "INVOICE_CREATE", "INVOICE_VOID", "INVOICE_EXPORT",
    "CREDIT_NOTE_CREATE", "CREDIT_NOTE_VOID",
    "CLIENT_CREATE", "CLIENT_IMPORT",
    "USER_MANAGEMENT", "COMPANY_SETTINGS",
    "ESTABLISHMENT_MANAGEMENT",
    "REPORT_OPERATIONAL", "REPORT_SALES", "REPORT_ADVANCED"
  ],
  "restrictions": ["NO API_ACCESS", "NO WEBHOOK_CONFIG"]
}
```

### Member/Accountant (Rol 2)
```json
{
  "roleId": 2,
  "features": [
    "INVOICE_CREATE", "INVOICE_EXPORT",
    "CREDIT_NOTE_CREATE",
    "CLIENT_CREATE",
    "REPORT_OPERATIONAL"
  ],
  "restrictions": ["NO USER_MANAGEMENT", "NO COMPANY_SETTINGS"]
}
```

---

## 📝 Changelog

### v1.0.0 (2025-12-18)
- ✅ Feature `ROLE_PERMISSION_MANAGEMENT` creado
- ✅ 4 endpoints implementados
- ✅ DTOs y servicios completados
- ✅ Documentación completa

---

## 🔗 Referencias

- [Feature Permission Implementation](./FEATURE_PERMISSION_IMPLEMENTATION.md)
- [Admin Role Permissions](./ADMIN_ROLE_PERMISSIONS.md)
- Migración: `V2025121704__add_role_permission_management_feature.sql`

---

**Fecha**: 2025-12-18  
**Feature Code**: `ROLE_PERMISSION_MANAGEMENT`  
**Status**: ✅ IMPLEMENTADO
