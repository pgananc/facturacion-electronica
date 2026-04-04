# 🔐 Configuración de Permisos para Rol Admin (id=3)

## 📋 Resumen Ejecutivo

Se han creado las migraciones Flyway necesarias para configurar los permisos del **rol Admin (id=3)** con acceso completo a nivel de compañía, pero sin acceso a administración global.

---

## 🎯 Objetivo

Configurar el rol **Admin** para que tenga:
- ✅ **Acceso completo** a funcionalidades de su compañía
- ✅ **Gestión de usuarios** de su compañía
- ✅ **Creación de facturas y reportes**
- ❌ **SIN acceso** a administración global (cross-company)
- ❌ **SIN acceso** a APIs y webhooks

---

## 📦 Archivos Creados

### 🆕 Migraciones Flyway

1. **`V2025121702__assign_admin_role_menus.sql`**
   - Asigna **28 menús** al rol Admin (id=3)
   - 7 grupos padres + 21 submenús
   - Excluye "Administración Global" (menús 16, 17, 18)

2. **`V2025121703__assign_admin_role_features.sql`**
   - Asigna **14 features** al rol Admin (id=3)
   - Cubre 5 módulos: Invoicing, Credit Notes, Clients, Products, Reports, Administration
   - Excluye `API_ACCESS` y `WEBHOOK_CONFIG`

### 🧪 Script de Validación

3. **`scripts/validate-admin-role-permissions.sql`**
   - 11 validaciones automáticas
   - Verifica menús, features, y exclusiones
   - Compara permisos entre roles

---

## 📊 Permisos Asignados

### 1️⃣ Menús (28 total)

#### Grupos Padre (7)
| ID | Nombre | Descripción |
|----|--------|-------------|
| 1 | Cliente | Gestión de clientes |
| 2 | Producto | Gestión de productos |
| 3 | Compañía | Configuración de compañía |
| 4 | Usuario | Gestión de usuarios |
| 5 | Facturación Electrónica | Facturas y notas de crédito |
| 20 | Reportes | Reportes operativos y comerciales |
| ❌ 16 | Administración Global | **EXCLUIDO - Solo super_admin** |

#### Submenús por Módulo (21)

**Cliente (1)**
- ID 8: Gestionar clientes

**Producto (1)**
- ID 9: Gestionar productos

**Compañía (3)**
- ID 10: Gestionar compañía
- ID 14: Gestionar sucursal
- ID 15: Gestionar Punto de Emisión

**Usuario (2)**
- ID 7: Gestionar usuarios por compañía
- ID 11: Gestionar usuario

**Facturación Electrónica (3)**
- ID 6: Facturas
- ID 12: Borradores
- ID 19: Nota de crédito

**Reportes (11)**
- ID 21: Reportes operativos (container)
- ID 22: Ventas por periodo
- ID 23: Ventas por cliente
- ID 24: Ventas por producto
- ID 25: Reporte de notas de crédito
- ID 26: Reporte Novedades (container)
- ID 27: Reporte de facturas pendientes
- ID 28: Reportes comerciales (container)
- ID 29: Dashboard de ventas
- ID 30: Comparativo por sucursal
- ID 31: Reporte de formas de pago

---

### 2️⃣ Features (14 total)

| Módulo | Feature Code | Descripción | Planes |
|--------|--------------|-------------|--------|
| **INVOICING** | `INVOICE_CREATE` | Crear facturas | FREE, BASIC, PRO, ENT |
| | `INVOICE_VOID` | Anular facturas | BASIC, PRO, ENT |
| | `INVOICE_EXPORT` | Exportar facturas | PRO, ENT |
| **CREDIT_NOTES** | `CREDIT_NOTE_CREATE` | Crear notas crédito | BASIC, PRO, ENT |
| | `CREDIT_NOTE_VOID` | Anular notas crédito | BASIC, PRO, ENT |
| **CLIENTS** | `CLIENT_CREATE` | Gestionar clientes | FREE, BASIC, PRO, ENT |
| | `CLIENT_IMPORT` | Importar clientes | PRO, ENT |
| **PRODUCTS** | `PRODUCT_MANAGEMENT` | Gestionar productos | FREE, BASIC, PRO, ENT |
| **REPORTS** | `REPORT_OPERATIONAL` | Reportes operativos | BASIC, PRO, ENT |
| | `REPORT_SALES` | Reportes comerciales | PRO, ENT |
| | `REPORT_ADVANCED` | Reportes avanzados | ENT |
| **ADMINISTRATION** | `USER_MANAGEMENT` | Gestión usuarios | BASIC, PRO, ENT |
| | `COMPANY_SETTINGS` | Config. compañía | FREE, BASIC, PRO, ENT |
| | `ESTABLISHMENT_MANAGEMENT` | Gestión sucursales | BASIC, PRO, ENT |

#### Features EXCLUIDOS (2)
| Feature Code | Razón |
|--------------|-------|
| ❌ `API_ACCESS` | Solo para super_admin y roles técnicos |
| ❌ `WEBHOOK_CONFIG` | Solo para super_admin |

---

## 🔄 Comparación de Roles

| Aspecto | Super Admin (id=1) | Admin (id=3) | Member (id=2) |
|---------|-------------------|--------------|---------------|
| **Menús totales** | ~31 | 28 | Por definir |
| **Features totales** | 16 | 14 | Por definir |
| **Adm. Global** | ✅ Sí | ❌ No | ❌ No |
| **API/Webhooks** | ✅ Sí | ❌ No | ❌ No |
| **Multi-empresa** | ✅ Sí | ❌ No | ❌ No |
| **Alcance** | Global | Compañía | Compañía |

---

## 🚀 Instalación y Validación

### Paso 1: Ejecutar Migraciones

Las migraciones se ejecutan **automáticamente** con Flyway al iniciar la aplicación:

```bash
./mvnw spring-boot:run
```

**Log esperado:**
```
INFO  FlywayMigrate - Migrating schema to version 2025.12.17.02
INFO  FlywayMigrate - Migrating schema to version 2025.12.17.03
INFO  FlywayMigrate - Successfully applied 2 migrations
```

### Paso 2: Validar con Script

```bash
psql postgresql://postgres:postgres@localhost:5433/ebill-dev-auth \
  -f scripts/validate-admin-role-permissions.sql
```

**Validaciones esperadas:**
```
✅ Menu Assignments: Expected 28 = Actual 28 - PASS
✅ Feature Assignments: Expected 14 = Actual 14 - PASS
✅ Excluded Menus: Expected 0 = Actual 0 - PASS
✅ Excluded Features: Expected 0 = Actual 0 - PASS
```

### Paso 3: Pruebas Manuales

#### Test 1: Obtener menú de usuario Admin
```bash
# Asumiendo que el usuario con id 5 (Ana Ceron) tiene rol Admin
curl -X GET http://localhost:8280/api/menu/1718529934 \
  -H "Authorization: Bearer <token>"
```

**Debe retornar 6 grupos** (sin "Administración Global"):
```json
[
  { "id": 1, "name": "Cliente", ... },
  { "id": 2, "name": "Producto", ... },
  { "id": 3, "name": "Compañía", ... },
  { "id": 4, "name": "Usuario", ... },
  { "id": 5, "name": "Facturación Electrónica", ... },
  { "id": 20, "name": "Reportes", ... }
]
```

#### Test 2: Obtener features permitidas del usuario
```bash
# Obtener todas las features permitidas para el usuario autenticado
curl -X GET http://localhost:8280/api/permission/user/features \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Response esperada:**
```json
[
  "USER_MANAGEMENT",
  "COMPANY_SETTINGS",
  "ESTABLISHMENT_MANAGEMENT"
]
```

#### Test 3: Validar feature antes de acción
```bash
# Validar acceso completo antes de ejecutar acción
curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "featureCode": "API_ACCESS",
    "idCompany": 1,
    "idUser": 5
  }'
```

**Response esperada (si no tiene acceso):**
```json
{
  "allowed": false,
  "reason": "PLAN_NOT_ALLOWED",
  "featureCode": "API_ACCESS",
  "message": "User does not have required role",
  "reason": "ROLE_NOT_ALLOWED"
}
```

---

## 🧪 Queries de Verificación

### Verificar menús asignados
```sql
SELECT COUNT(*) as total_menus
FROM menu_role 
WHERE id_role = 3 AND status = true;
-- Expected: 28
```

### Verificar features asignados
```sql
SELECT COUNT(*) as total_features
FROM feature_permission 
WHERE 3 = ANY(allowed_roles) AND is_active = true;
-- Expected: 14
```

### Ver jerarquía de menús del Admin
```sql
WITH RECURSIVE menu_tree AS (
    SELECT m.id_menu, m.name, m.url, m.parent_id_menu, 0 as level
    FROM menu m
    INNER JOIN menu_role mr ON mr.id_menu = m.id_menu
    WHERE mr.id_role = 3 AND mr.status = true AND m.parent_id_menu IS NULL
    
    UNION ALL
    
    SELECT m.id_menu, m.name, m.url, m.parent_id_menu, mt.level + 1
    FROM menu m
    INNER JOIN menu_role mr ON mr.id_menu = m.id_menu
    INNER JOIN menu_tree mt ON m.parent_id_menu = mt.id_menu
    WHERE mr.id_role = 3 AND mr.status = true
)
SELECT 
    REPEAT('  ', level) || name as hierarchy,
    url
FROM menu_tree
ORDER BY id_menu;
```

### Comparar permisos entre roles
```sql
SELECT 
    fp.feature_code,
    CASE WHEN 1 = ANY(fp.allowed_roles) THEN '✓' ELSE '✗' END as super_admin,
    CASE WHEN 2 = ANY(fp.allowed_roles) THEN '✓' ELSE '✗' END as member,
    CASE WHEN 3 = ANY(fp.allowed_roles) THEN '✓' ELSE '✗' END as admin
FROM feature_permission fp
WHERE fp.is_active = true
ORDER BY fp.module, fp.feature_code;
```

---

## 🎯 Casos de Uso del Admin

### ✅ Lo que puede hacer:

1. **Gestión de su Compañía**
   - Configurar datos de la compañía
   - Crear sucursales y puntos de emisión
   - Configurar parámetros

2. **Gestión de Usuarios**
   - Crear usuarios dentro de su compañía
   - Asignar roles (miembro, operador)
   - Gestionar permisos de usuarios

3. **Operaciones Diarias**
   - Crear y anular facturas
   - Crear y anular notas de crédito
   - Gestionar clientes y productos
   - Gestionar borradores

4. **Reportes y Análisis**
   - Ver reportes operativos
   - Analizar ventas y tendencias
   - Usar dashboards avanzados
   - Comparar sucursales

### ❌ Lo que NO puede hacer:

1. **Administración Global**
   - Ver datos de otras compañías
   - Gestionar establecimientos de otras compañías
   - Acceder a configuraciones globales

2. **Integraciones Técnicas**
   - Configurar acceso API
   - Configurar webhooks
   - Generar tokens de API

---

## 📝 Datos de Prueba

### Usuarios con rol Admin (id=3)

Según los datos iniciales:

```sql
-- Ana Ceron - Admin (user_company_role id=5)
SELECT u.username, u.name, r.name as role
FROM users u
INNER JOIN user_company uc ON uc.id_user = u.id_user
INNER JOIN user_company_role ucr ON ucr.id_user_company = uc.id_user_company
INNER JOIN role r ON r.id_role = ucr.id_role
WHERE r.id_role = 3;

-- Username: 1718529934
-- Name: Ana Ceron
-- Mail: anny.ceron@gmail.com
```

Para testing, usar:
- **Username**: `1718529934`
- **Password**: `123` (default password)

---

## 🐛 Troubleshooting

### Problema: Admin ve "Administración Global"
```sql
-- Verificar que no tenga acceso
SELECT * FROM menu_role 
WHERE id_role = 3 AND id_menu IN (16, 17, 18);
-- Debe retornar 0 filas

-- Si retorna filas, eliminar
DELETE FROM menu_role 
WHERE id_role = 3 AND id_menu IN (16, 17, 18);
```

### Problema: Admin puede acceder a API_ACCESS
```sql
-- Verificar que no esté en allowed_roles
SELECT feature_code, allowed_roles 
FROM feature_permission 
WHERE feature_code = 'API_ACCESS';
-- No debe incluir 3 en el array

-- Si lo incluye, corregir
UPDATE feature_permission 
SET allowed_roles = array_remove(allowed_roles, 3)
WHERE feature_code IN ('API_ACCESS', 'WEBHOOK_CONFIG');
```

### Problema: Falta algún menú
```sql
-- Ver qué menús faltan
SELECT m.id_menu, m.name 
FROM menu m
WHERE m.id_menu IN (1,2,3,4,5,6,7,8,9,10,11,12,14,15,19,20,21,22,23,24,25,26,27,28,29,30,31)
  AND NOT EXISTS (
    SELECT 1 FROM menu_role mr 
    WHERE mr.id_menu = m.id_menu AND mr.id_role = 3
  );

-- Agregar el menú faltante manualmente
INSERT INTO menu_role (id_menu_role, id_menu, id_role, status)
VALUES (nextval('menu_role_seq'), <ID_MENU>, 3, true);
```

### Problema: Error "duplicate key value violates unique constraint menu_role_pkey"
Este error ocurre cuando la secuencia está desincronizada.

```sql
-- Sincronizar la secuencia con el máximo ID actual
SELECT setval('menu_role_seq', (SELECT MAX(id_menu_role) FROM menu_role));

-- Verificar el valor actual de la secuencia
SELECT currval('menu_role_seq');

-- Ahora reintentar la migración
```

**Nota**: La migración V2025121702 ya incluye un fix automático para esto al inicio del script.

---

## ✅ Checklist de Validación

- [ ] Migraciones ejecutadas exitosamente
- [ ] Script de validación corrido sin errores
- [ ] 28 menús asignados al rol Admin
- [ ] 14 features asignados al rol Admin
- [ ] Admin NO tiene menús 16, 17, 18
- [ ] Admin NO tiene features API_ACCESS ni WEBHOOK_CONFIG
- [ ] Endpoint `/api/menu/{username}` retorna 6 grupos para Admin
- [ ] Validación de features funciona correctamente
- [ ] Frontend recibe menús correctos
- [ ] Plan-based validation funciona

---

## 📚 Referencias

- [Implementación Feature Code en Menús](./MENU_FEATURE_CODE_IMPLEMENTATION.md)
- [Feature Permission Quick Reference](./FEATURE_PERMISSION_QUICK_REF.md)
- Migración: `V2025121702__assign_admin_role_menus.sql`
- Migración: `V2025121703__assign_admin_role_features.sql`
- Validación: `scripts/validate-admin-role-permissions.sql`

---

**Estado**: ✅ Implementación COMPLETA
**Fecha**: 2025-12-17
**Siguiente**: Testing en QA
