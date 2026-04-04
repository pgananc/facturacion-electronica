# 🎯 Feature Code en Menús - Implementación

## 📋 Resumen

Implementación del campo `feature_code` en la tabla `menu` para permitir validación de permisos basada en planes (subscripciones) desde el frontend.

## 🎯 Objetivo

Permitir que el **frontend** filtre el menú basándose en:
1. **Rol del usuario** (Layer 1 - Ya existía con `menu_role`)
2. **Plan de la compañía** (Layer 2 - **NUEVO** con `feature_code`)

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────┐
│ BACKEND (ebill-authentication)                          │
├─────────────────────────────────────────────────────────┤
│ 1. Usuario solicita menú: GET /api/menu/{username}     │
│ 2. Se filtra por ROL (user → role → menu_role → menu) │
│ 3. Response incluye featureCode en cada menú           │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ FRONTEND                                                │
├─────────────────────────────────────────────────────────┤
│ 4. Recibe menús con featureCode                        │
│ 5. Para cada menú con featureCode:                     │
│    - POST /api/permission/validate-feature             │
│    - Body: { featureCode, idCompany }                  │
│ 6. Backend valida: ROL + PLAN                          │
│ 7. Frontend muestra/oculta según respuesta             │
└─────────────────────────────────────────────────────────┘
```

## 📂 Archivos Modificados

### 1. **Migración Flyway**
```
src/main/resources/flyway/V2025121701__add_feature_code_to_menu.sql
```
- Agrega columna `feature_code VARCHAR(50)` a tabla `menu`
- Crea FK constraint hacia `feature_permission(feature_code)`
- Crea índice para performance
- Pobla feature codes para menús existentes
- Agrega feature `PRODUCT_MANAGEMENT` que faltaba

### 2. **Entidad Java**
```
src/main/java/com/wallparisoft/authentication/entity/Menu.java
```
- Agrega campo `private String featureCode;`

### 3. **DTO**
```
src/main/java/com/wallparisoft/authentication/dto/MenuDto.java
```
- Agrega campo `private String featureCode;`

### 4. **Mapper**
```
src/main/java/com/wallparisoft/authentication/mapper/MenuMapper.java
```
- Agrega mapping explícito para `featureCode`

### 5. **Service**
```
src/main/java/com/wallparisoft/authentication/service/impl/MenuServiceImpl.java
```
- Extrae `featureCode` del query result (row[6])
- Lo asigna al objeto `Menu`

### 6. **Script de Validación**
```
scripts/validate-menu-feature-integrity.sql
```
- Valida integridad de datos post-migración
- Detecta problemas comunes
- Muestra jerarquía de menús completa

## 🗂️ Mapeo de Menús a Feature Codes

| Menu ID | Nombre del Menú | URL | Feature Code |
|---------|-----------------|-----|--------------|
| 6 | Facturas | `/billing` | `INVOICE_CREATE` |
| 19 | Nota de crédito | `/credit-note` | `CREDIT_NOTE_CREATE` |
| 8 | Gestionar clientes | `/client` | `CLIENT_CREATE` |
| 7 | Gestionar usuarios por compañía | `/company-user` | `USER_MANAGEMENT` |
| 11 | Gestionar usuario | `/user` | `USER_MANAGEMENT` |
| 10 | Gestionar compañía | `/company` | `COMPANY_SETTINGS` |
| 12-15 | Establecimiento/Sucursal/Punto emisión | varios | `ESTABLISHMENT_MANAGEMENT` |
| 9 | Gestionar productos | `/product` | `PRODUCT_MANAGEMENT` |
| 22-25,27 | Reportes operativos | varios | `REPORT_OPERATIONAL` |
| 29-30 | Dashboard/Comparativo ventas | varios | `REPORT_ADVANCED` |
| 31 | Formas de pago | `/reports/payment-methods` | `REPORT_SALES` |

### Menús SIN feature_code (Contenedores)

Los siguientes son **menús padres/contenedores** que no tienen URL y por lo tanto no necesitan `feature_code`:

- **ID 1**: Cliente (contenedor)
- **ID 2**: Producto (contenedor)
- **ID 3**: Compañía (contenedor)
- **ID 4**: Usuario (contenedor)
- **ID 5**: Facturación Electrónica (contenedor)
- **ID 20**: Reportes (contenedor)
- **ID 21**: Reportes operativos (contenedor)
- **ID 26**: Reporte Novedades (contenedor)
- **ID 28**: Reportes comerciales (contenedor)

**Lógica Frontend**: Los contenedores se muestran si al menos **uno** de sus hijos es accesible.

## 🚀 Pasos de Implementación

### 1. Ejecutar Migración

La migración se ejecuta **automáticamente** con Flyway al iniciar la aplicación:

```bash
# Reiniciar el servicio
./mvnw spring-boot:run
```

Flyway ejecutará `V2025121701__add_feature_code_to_menu.sql` automáticamente.

### 2. Validar Integridad

Ejecutar el script de validación manualmente:

```bash
psql postgresql://postgres:postgres@localhost:5433/ebill-dev-auth \
  -f scripts/validate-menu-feature-integrity.sql
```

**Verificaciones que hace:**
- ✅ No hay feature codes huérfanos (referencias inválidas)
- ✅ Todos los menús de acción tienen feature_code
- ✅ FK constraint existe
- ✅ No hay features inactivas en uso

### 3. Probar el Endpoint

```bash
# Obtener menú de un usuario
curl -X GET http://localhost:8280/api/menu/1722633961 \
  -H "Authorization: Bearer <token>"
```

**Response esperado:**
```json
[
  {
    "id": 5,
    "name": "Facturación Electrónica",
    "icon": "store",
    "url": null,
    "featureCode": null,
    "subMenus": [
      {
        "id": 6,
        "name": "Facturas",
        "icon": "store",
        "url": "/billing",
        "featureCode": "INVOICE_CREATE",
        "subMenus": []
      },
      {
        "id": 19,
        "name": "Nota de crédito",
        "icon": "credit_card",
        "url": "/credit-note",
        "featureCode": "CREDIT_NOTE_CREATE",
        "subMenus": []
      }
    ]
  }
]
```

### 4. Verificar en Base de Datos

```sql
-- Ver todos los menús con feature codes
SELECT 
    m.id_menu,
    m.name,
    m.url,
    m.feature_code,
    fp.feature_name
FROM menu m
LEFT JOIN feature_permission fp ON m.feature_code = fp.feature_code
WHERE m.status = true
ORDER BY m.id_menu;

-- Ver jerarquía completa
WITH RECURSIVE menu_tree AS (
    SELECT id_menu, name, url, feature_code, parent_id_menu, 0 as level
    FROM menu WHERE parent_id_menu IS NULL AND status = true
    UNION ALL
    SELECT m.id_menu, m.name, m.url, m.feature_code, m.parent_id_menu, mt.level + 1
    FROM menu m
    INNER JOIN menu_tree mt ON m.parent_id_menu = mt.id_menu
    WHERE m.status = true
)
SELECT 
    REPEAT('  ', level) || name as hierarchy,
    url,
    feature_code
FROM menu_tree
ORDER BY id_menu;
```

## 🧪 Testing

### Test Case 1: Usuario con Rol pero sin Plan
```bash
# Usuario: Super Admin (rol 1)
# Compañía: Plan FREE
# Feature: INVOICE_EXPORT (requiere PROFESSIONAL)

# 1. Obtener menú (debe aparecer porque tiene el rol)
curl -X GET http://localhost:8280/api/menu/username

# 2. Intentar validar feature
curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Content-Type: application/json" \
  -d '{
    "featureCode": "INVOICE_EXPORT",
    "idCompany": 1
  }'

# Response esperada: 403 FORBIDDEN
{
  "allowed": false,
  "reason": "PLAN_DOES_NOT_INCLUDE_FEATURE",
  "message": "Your plan does not include this feature",
  "upgradeUrl": "https://..."
}
```

### Test Case 2: Usuario con Plan correcto
```bash
# Usuario: Super Admin (rol 1)
# Compañía: Plan PROFESSIONAL
# Feature: INVOICE_EXPORT

curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Content-Type: application/json" \
  -d '{
    "featureCode": "INVOICE_EXPORT",
    "idCompany": 1
  }'

# Response esperada: 200 OK
{
  "allowed": true,
  "reason": "ACCESS_GRANTED",
  "message": "Access granted to feature INVOICE_EXPORT"
}
```

## 📊 Beneficios

### ✅ Antes (Solo validación por Rol)
```
Backend: Filtra menú por ROL → Frontend: Muestra todo
```
**Problema**: Usuario ve menús de features no incluidas en su plan.

### ✅ Ahora (Validación por Rol + Plan)
```
Backend: Filtra menú por ROL → Frontend: Valida PLAN → Oculta si no tiene acceso
```
**Solución**: Usuario solo ve menús que puede usar realmente.

## 🔒 Seguridad

### Capas de Seguridad
1. **Backend - Menu Query**: Filtra por ROL (SQL query con JOIN a `menu_role`)
2. **Backend - Feature Validation**: Valida ROL + PLAN (endpoint `/api/permission/validate-feature`)
3. **Frontend - Guard**: Bloquea navegación (PermissionGuard llama a backend)
4. **Backend - Endpoints**: Valida JWT + permisos (cada endpoint protegido)

**Conclusión**: Ocultar menús en frontend es **UX**, la seguridad real está en backend.

## 🐛 Troubleshooting

### Problema: Menú no aparece después de migración
```sql
-- Verificar que el menú tiene feature_code
SELECT id_menu, name, feature_code FROM menu WHERE id_menu = X;

-- Verificar que el feature existe
SELECT * FROM feature_permission WHERE feature_code = 'CODIGO';

-- Verificar que el rol tiene acceso
SELECT * FROM menu_role WHERE id_menu = X AND id_role = Y;
```

### Problema: Frontend no recibe featureCode
```bash
# Verificar response del endpoint
curl -X GET http://localhost:8280/api/menu/username | jq

# Si no aparece, verificar que MenuMapper está correcto
# y que MenuServiceImpl extrae row[6]
```

### Problema: FK constraint error
```sql
-- Esto significa que hay un feature_code que no existe en feature_permission
SELECT m.id_menu, m.feature_code 
FROM menu m 
LEFT JOIN feature_permission fp ON m.feature_code = fp.feature_code
WHERE m.feature_code IS NOT NULL AND fp.feature_code IS NULL;

-- Corregir insertando el feature que falta o cambiando el feature_code
```

## 📝 Agregar Nuevo Menú (Post-Implementación)

Para agregar un nuevo menú CON feature validation:

```sql
-- 1. Asegurarse de que el feature existe
INSERT INTO feature_permission (feature_code, feature_name, description, module, allowed_roles, required_plans, is_active)
VALUES ('NEW_FEATURE', 'Nueva Feature', 'Descripción', 'MODULE', ARRAY[1,2], ARRAY['BASIC','PROFESSIONAL','ENTERPRISE'], true);

-- 2. Insertar el menú con feature_code
INSERT INTO menu (id_menu, parent_id_menu, icon, name, url, status, feature_code)
VALUES (99, 5, 'icon_name', 'Nuevo Menú', '/new-url', true, 'NEW_FEATURE');

-- 3. Asignar roles al menú
INSERT INTO menu_role (id_menu_role, id_menu, id_role, status)
VALUES (99, 99, 1, true); -- Super Admin
```

**¡No se requiere deploy del frontend!** 🎉

## 🎉 Resultado Final

- ✅ Menús incluyen `featureCode` en la respuesta JSON
- ✅ Frontend puede validar acceso con `/api/permission/validate-feature`
- ✅ Sistema escalable: agregar menús desde DB sin código
- ✅ Integridad garantizada: FK constraint previene errores
- ✅ Fácil mantenimiento: todo centralizado en base de datos
