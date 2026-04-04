# 📋 Documentación API - CRUD de Menús

## 🔐 Autenticación
Todos los endpoints requieren autenticación JWT y rol `super_admin`.

**Header requerido:**
```bash
Authorization: Bearer <JWT_TOKEN>
```

---

## 📍 Endpoints

### 1. GET `/api/admin/menu` - Listar todos los menús (árbol completo)

**Descripción:** Obtiene todos los menús en estructura jerárquica completa con sus features asociados.

**Request:**
```bash
curl -X GET "http://localhost:8280/api/admin/menu" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json"
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Cliente",
    "icon": "person",
    "url": null,
    "featureCode": null,
    "feature": null,
    "parentId": null,
    "active": true,
    "order": 1,
    "subMenus": [
      {
        "id": 8,
        "name": "Gestionar clientes",
        "icon": "person",
        "url": "/clients",
        "featureCode": "CLIENT_CREATE",
        "feature": {
          "featureCode": "CLIENT_CREATE",
          "featureName": "Crear Clientes",
          "description": "Crear y gestionar clientes",
          "module": "CLIENTS",
          "allowedRoles": [1, 2, 3],
          "requiredPlans": ["FREE", "BASIC", "PROFESSIONAL", "ENTERPRISE"],
          "isActive": true
        },
        "parentId": 1,
        "active": true,
        "order": 1,
        "subMenus": []
      }
    ]
  },
  {
    "id": 5,
    "name": "Facturación Electrónica",
    "icon": "store",
    "url": null,
    "featureCode": null,
    "feature": null,
    "parentId": null,
    "active": true,
    "order": 5,
    "subMenus": [
      {
        "id": 6,
        "name": "Facturas",
        "icon": "store",
        "url": "/invoices",
        "featureCode": "INVOICE_CREATE",
        "feature": {
          "featureCode": "INVOICE_CREATE",
          "featureName": "Crear Facturas",
          "description": "Crear y emitir facturas electrónicas",
          "module": "INVOICING",
          "allowedRoles": [1, 2, 3],
          "requiredPlans": ["BASIC", "PROFESSIONAL", "ENTERPRISE"],
          "isActive": true
        },
        "parentId": 5,
        "active": true,
        "order": 2,
        "subMenus": []
      }
    ]
  }
]
```

---

### 2. GET `/api/admin/menu/{id}` - Obtener un menú específico

**Descripción:** Obtiene un menú por su ID con su árbol completo de submenús y feature asociado.

**Request:**
```bash
curl -X GET "http://localhost:8280/api/admin/menu/6" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json"
```

**Response (200 OK):**
```json
{
  "id": 6,
  "name": "Facturas",
  "icon": "store",
  "url": "/invoices",
  "featureCode": "INVOICE_CREATE",
  "feature": {
    "featureCode": "INVOICE_CREATE",
    "featureName": "Crear Facturas",
    "description": "Crear y emitir facturas electrónicas",
    "module": "INVOICING",
    "allowedRoles": [1, 2, 3],
    "requiredPlans": ["BASIC", "PROFESSIONAL", "ENTERPRISE"],
    "isActive": true
  },
  "parentId": 5,
  "active": true,
  "order": 2,
  "subMenus": []
}
```

**Response (404 Not Found):**
```json
{
  "message": "Menú no encontrado",
  "error": "NOT_FOUND"
}
```

---

### 3. POST `/api/admin/menu` - Crear nuevo menú

**Descripción:** Crea un nuevo menú. Campos requeridos: `name`, `icon`, `active`.

**Request:**
```bash
curl -X POST "http://localhost:8280/api/admin/menu" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nuevo Menú",
    "icon": "settings",
    "url": "/new-menu",
    "featureCode": "INVOICE_CREATE",
    "parentId": null,
    "active": true,
    "order": 10
  }'
```

**Response (201 Created):**
```json
{
  "id": 35,
  "name": "Nuevo Menú",
  "icon": "settings",
  "url": "/new-menu",
  "featureCode": "INVOICE_CREATE",
  "feature": {
    "featureCode": "INVOICE_CREATE",
    "featureName": "Crear Facturas",
    "description": "Crear y emitir facturas electrónicas",
    "module": "INVOICING",
    "allowedRoles": [1, 2, 3],
    "requiredPlans": ["BASIC", "PROFESSIONAL", "ENTERPRISE"],
    "isActive": true
  },
  "parentId": null,
  "active": true,
  "order": 10,
  "subMenus": []
}
```

**Response (400 Bad Request) - Campos requeridos faltantes:**
```json
{
  "message": "El nombre del menú es requerido",
  "error": "VALIDATION_ERROR"
}
```

**Response (400 Bad Request) - Feature code inválido:**
```json
{
  "message": "Feature code no encontrado o inactivo: INVALID_FEATURE",
  "error": "VALIDATION_ERROR"
}
```

---

### 4. PUT `/api/admin/menu/{id}` - Actualizar menú (PATCH parcial)

**Descripción:** Actualiza un menú existente. Solo se actualizan los campos que vienen en el request (actualización parcial).

**Ejemplo 1: Actualizar solo el nombre**
```bash
curl -X PUT "http://localhost:8280/api/admin/menu/6" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Facturas Actualizadas"
  }'
```

**Response (200 OK):**
```json
{
  "id": 6,
  "name": "Facturas Actualizadas",
  "icon": "store",
  "url": "/invoices",
  "featureCode": "INVOICE_CREATE",
  "feature": {
    "featureCode": "INVOICE_CREATE",
    "featureName": "Crear Facturas",
    "description": "Crear y emitir facturas electrónicas",
    "module": "INVOICING",
    "allowedRoles": [1, 2, 3],
    "requiredPlans": ["BASIC", "PROFESSIONAL", "ENTERPRISE"],
    "isActive": true
  },
  "parentId": 5,
  "active": true,
  "order": 2,
  "subMenus": []
}
```

**Ejemplo 2: Actualizar feature code**
```bash
curl -X PUT "http://localhost:8280/api/admin/menu/6" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "featureCode": "CREDIT_NOTE_CREATE"
  }'
```

**Response (200 OK):**
```json
{
  "id": 6,
  "name": "Facturas",
  "icon": "store",
  "url": "/invoices",
  "featureCode": "CREDIT_NOTE_CREATE",
  "feature": {
    "featureCode": "CREDIT_NOTE_CREATE",
    "featureName": "Crear Notas de Crédito",
    "description": "Crear y emitir notas de crédito",
    "module": "CREDIT_NOTES",
    "allowedRoles": [1, 2, 3],
    "requiredPlans": ["BASIC", "PROFESSIONAL", "ENTERPRISE"],
    "isActive": true
  },
  "parentId": 5,
  "active": true,
  "order": 2,
  "subMenus": []
}
```

**Ejemplo 3: Quitar feature code (enviar string vacío)**
```bash
curl -X PUT "http://localhost:8280/api/admin/menu/6" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "featureCode": ""
  }'
```

**Ejemplo 4: Actualizar múltiples campos**
```bash
curl -X PUT "http://localhost:8280/api/admin/menu/6" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Facturas",
    "icon": "receipt",
    "url": "/invoices",
    "order": 1,
    "active": true
  }'
```

**Ejemplo 5: Cambiar parent (mover a otro grupo)**
```bash
curl -X PUT "http://localhost:8280/api/admin/menu/6" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "parentId": 1
  }'
```

**Ejemplo 6: Quitar parent (convertir en menú raíz)**
```bash
curl -X PUT "http://localhost:8280/api/admin/menu/6" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "parentId": 0
  }'
```

**Response (400 Bad Request) - Ciclo detectado:**
```json
{
  "message": "No se puede asignar un descendiente como padre del menú",
  "error": "VALIDATION_ERROR"
}
```

---

### 5. DELETE `/api/admin/menu/{id}/hard` - Eliminar menú físicamente (hard delete)

**Descripción:** Elimina físicamente un menú y todos sus registros relacionados en cascada. Elimina:
- Todos los registros en `menu_role` asociados al menú
- El `feature_permission` si solo este menú lo está usando
- El menú mismo
- Todos los submenús recursivamente

**Request:**
```bash
curl -X DELETE "http://localhost:8280/api/admin/menu/6/hard" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json"
```

**Response (204 No Content):**
```
(No body)
```

**Response (404 Not Found):**
```json
{
  "message": "Menú no encontrado",
  "error": "NOT_FOUND"
}
```

---

## 🔒 Respuestas de Error Comunes

### 403 Forbidden - Sin permisos
```json
{
  "message": "No tiene permisos para gestionar menús",
  "error": "FORBIDDEN"
}
```

### 400 Bad Request - Validación
```json
{
  "message": "El nombre del menú no puede estar vacío",
  "error": "VALIDATION_ERROR"
}
```

---

## 📝 Notas Importantes

1. **Actualización Parcial (PATCH):** El endpoint `PUT` solo actualiza los campos que vienen en el request. Los campos que no se envían permanecen sin cambios.

2. **Feature Permission:** Cuando un menú tiene `featureCode`, el objeto `feature` completo se carga automáticamente con toda la información del `FeaturePermission` asociado.

3. **Hard Delete:** El endpoint `DELETE /{id}/hard` elimina físicamente el menú y todos sus registros relacionados en cascada.

4. **Validaciones:**
   - No se puede eliminar un menú con submenús activos
   - No se puede asignar un descendiente como padre (evita ciclos)
   - El `featureCode` debe existir y estar activo en `feature_permission`

5. **Campos Opcionales en Request:**
   - `url`: Puede ser `null`, string vacío `""`, o comenzar con `/`
   - `featureCode`: Puede ser `null` o string vacío para quitar el feature
   - `parentId`: Puede ser `null` o `0` para quitar el padre
   - `order`: Puede ser `null`

6. **Campos Requeridos para Crear:**
   - `name`: Requerido
   - `icon`: Requerido
   - `active`: Requerido

---

## 🧪 Ejemplos de Testing

### Crear menú completo con feature
```bash
curl -X POST "http://localhost:8280/api/admin/menu" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gestión de Reportes",
    "icon": "assessment",
    "url": "/reports",
    "featureCode": "REPORT_OPERATIONAL",
    "parentId": 20,
    "active": true,
    "order": 1
  }'
```

### Actualizar solo el orden
```bash
curl -X PUT "http://localhost:8280/api/admin/menu/6" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "order": 5
  }'
```

### Eliminar menú físicamente (hard delete)
```bash
curl -X DELETE "http://localhost:8280/api/admin/menu/6/hard" \
  -H "Authorization: Bearer YOUR_TOKEN"
```
