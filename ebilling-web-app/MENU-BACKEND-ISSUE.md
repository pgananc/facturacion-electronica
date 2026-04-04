# 🚨 Problema: Backend Solo Envía 1 Menú Vacío

## ✅ Diagnóstico Completado

### Problema Identificado

El backend solo está enviando:

```javascript
{
  id: 3,
  name: 'Compañía',
  icon: 'store',
  url: 'null',
  subMenus: []  // ❌ VACÍO
}
```

**Debería enviar todos los grupos:**

- Facturación
- Notas de Crédito
- Clientes
- Productos
- Reportes
- Usuarios
- Establecimientos
- Compañías
- Configuración

---

## 🔍 Verificar en Backend

### Endpoint del Menú

**Request actual:**

```bash
GET http://localhost:8280/api/menu/0102030405
Authorization: Bearer <token>
```

**Response actual (INCORRECTO):**

```json
[
  {
    "id": 3,
    "name": "Compañía",
    "icon": "store",
    "url": "null",
    "subMenus": []
  }
]
```

**Response esperado (CORRECTO):**

```json
[
  {
    "id": 1,
    "name": "Facturación",
    "icon": "receipt",
    "url": null,
    "subMenus": [
      {
        "id": 10,
        "name": "Nueva Factura",
        "icon": "add_circle",
        "url": "/invoices"
      },
      {
        "id": 11,
        "name": "Lista de Facturas",
        "icon": "list",
        "url": "/invoices/list"
      }
    ]
  },
  {
    "id": 2,
    "name": "Notas de Crédito",
    "icon": "credit_card",
    "url": null,
    "subMenus": [
      {
        "id": 20,
        "name": "Nueva Nota de Crédito",
        "icon": "add",
        "url": "/credit-notes"
      }
    ]
  },
  {
    "id": 3,
    "name": "Clientes",
    "icon": "people",
    "url": null,
    "subMenus": [
      {
        "id": 30,
        "name": "Lista de Clientes",
        "icon": "list",
        "url": "/clients"
      }
    ]
  },
  {
    "id": 4,
    "name": "Reportes",
    "icon": "assessment",
    "url": null,
    "subMenus": [
      {
        "id": 40,
        "name": "Ventas por Período",
        "icon": "trending_up",
        "url": "/reports/sales"
      },
      {
        "id": 41,
        "name": "Dashboard Avanzado",
        "icon": "dashboard",
        "url": "/reports/sales-dashboard"
      },
      {
        "id": 42,
        "name": "Comparativa de Sucursales",
        "icon": "compare_arrows",
        "url": "/reports/branch-comparison"
      }
    ]
  },
  {
    "id": 5,
    "name": "Usuarios",
    "icon": "person",
    "url": null,
    "subMenus": [
      {
        "id": 50,
        "name": "Gestión de Usuarios",
        "icon": "manage_accounts",
        "url": "/users"
      }
    ]
  },
  {
    "id": 6,
    "name": "Establecimientos",
    "icon": "business",
    "url": null,
    "subMenus": [
      {
        "id": 60,
        "name": "Lista de Establecimientos",
        "icon": "store",
        "url": "/establishments"
      },
      {
        "id": 61,
        "name": "Puntos de Emisión",
        "icon": "point_of_sale",
        "url": "/invoice-points"
      }
    ]
  },
  {
    "id": 7,
    "name": "Compañías",
    "icon": "business_center",
    "url": null,
    "subMenus": [
      {
        "id": 70,
        "name": "Información de Compañía",
        "icon": "info",
        "url": "/companies"
      }
    ]
  }
]
```

---

## 🎯 Soluciones

### Opción 1: Backend Envía TODO (Recomendado) ✅

**Dejar que el frontend filtre** usando `MenuFilterService`.

#### En Backend (Java)

**Archivo:** `MenuService.java` o `MenuController.java`

**ANTES (filtra por usuario):**

```java
@GetMapping("/menu/{identification}")
public ResponseEntity<List<Menu>> getMenus(@PathVariable String identification) {
    // ❌ Ya filtra en backend
    List<Menu> menus = menuService.findByUserIdentification(identification);
    return ResponseEntity.ok(menus);
}
```

**AHORA (envía todos):**

```java
@GetMapping("/menu/{identification}")
public ResponseEntity<List<Menu>> getMenus(@PathVariable String identification) {
    // ✅ Envía TODOS los menús
    List<Menu> menus = menuService.findAll(); // O findAllActive()
    return ResponseEntity.ok(menus);
}
```

**O si necesitas validar que el usuario esté autenticado:**

```java
@GetMapping("/menu/{identification}")
public ResponseEntity<List<Menu>> getMenus(
    @PathVariable String identification,
    @RequestHeader("Authorization") String token
) {
    // Validar token
    if (!jwtUtil.validateToken(token)) {
        return ResponseEntity.status(401).build();
    }

    // Enviar TODOS los menús (sin filtrar por rol)
    List<Menu> menus = menuService.findAllActive();
    return ResponseEntity.ok(menus);
}
```

---

### Opción 2: Backend Filtra por Rol + Plan

**El backend hace el filtrado** consultando la suscripción.

#### Ventajas

- Menos lógica en frontend
- Más seguro (no expone menús no permitidos)

#### Desventajas

- Más complejo en backend
- Requiere consultar servicio de suscripción
- Cada cambio de menú requiere cambio en backend

#### Implementación (Java)

```java
@GetMapping("/menu/{identification}")
public ResponseEntity<List<Menu>> getMenus(@PathVariable String identification) {
    // 1. Obtener usuario
    User user = userService.findByIdentification(identification);

    // 2. Obtener suscripción
    Subscription subscription = subscriptionClient.getByCompanyId(user.getIdCompany());

    // 3. Filtrar menús por rol + plan
    List<Menu> allMenus = menuService.findAll();
    List<Menu> filteredMenus = menuFilterService.filterByRoleAndPlan(
        allMenus,
        user.getRoles(),
        subscription.getPlan().getCode()
    );

    return ResponseEntity.ok(filteredMenus);
}
```

---

## 🔧 Testing del Endpoint

### Test 1: Verificar Response Actual

```bash
# Con tu token actual
curl -X GET http://localhost:8280/api/menu/0102030405 \
  -H "Authorization: Bearer <tu-token>" \
  | jq
```

**Resultado esperado:**

- Debería mostrar TODOS los grupos de menú
- Cada grupo debe tener `subMenus` con al menos 1 item

---

### Test 2: Verificar en Base de Datos

```sql
-- Ver todos los menús disponibles
SELECT id, name, icon, url, parent_id
FROM menu
WHERE active = true
ORDER BY id;

-- Ver la jerarquía completa
SELECT
  parent.id AS parent_id,
  parent.name AS parent_name,
  child.id AS child_id,
  child.name AS child_name,
  child.url AS child_url
FROM menu parent
LEFT JOIN menu child ON child.parent_id = parent.id
WHERE parent.active = true
  AND parent.parent_id IS NULL
ORDER BY parent.id, child.id;
```

**Resultado esperado:**

- Debería haber al menos 7-8 grupos padre (parent_id = NULL)
- Cada grupo debe tener al menos 1 hijo (submenú)

---

## ✅ Verificación en Frontend

Una vez que el backend envíe todos los menús, deberías ver:

### En Console:

```
📋 [MenuFilter] Menús recibidos del backend: 7 grupos  👈 NO 1!
✅ [MenuFilter] Menús después de filtrar: 5 grupos  👈 Filtró 2 (para FREE)
```

### En Network Tab:

```json
// Response de GET /api/menu/0102030405
[
  { "id": 1, "name": "Facturación", "subMenus": [...] },
  { "id": 2, "name": "Notas de Crédito", "subMenus": [...] },
  { "id": 3, "name": "Clientes", "subMenus": [...] },
  { "id": 4, "name": "Reportes", "subMenus": [...] },
  { "id": 5, "name": "Usuarios", "subMenus": [...] },
  { "id": 6, "name": "Establecimientos", "subMenus": [...] },
  { "id": 7, "name": "Compañías", "subMenus": [...] }
]
```

---

## 📋 Checklist

### Backend

- [ ] Endpoint `/api/menu/{id}` devuelve TODOS los menús activos
- [ ] Cada grupo padre tiene `subMenus` array poblado
- [ ] Los submenús tienen `url` válidas (ej: `/invoices`, `/clients`)
- [ ] No filtra por rol (deja que el frontend lo haga)

### Frontend

- [ ] Los logs muestran "X grupos recibidos" donde X >= 7
- [ ] Los logs muestran "Y grupos después de filtrar" donde Y < X
- [ ] El menú lateral muestra las opciones correctas según el plan

---

## 🆘 Si Necesitas Ayuda

Envía esta información:

1. **Response del endpoint:**

```bash
curl -X GET http://localhost:8280/api/menu/0102030405 \
  -H "Authorization: Bearer <token>" \
  | jq > menu-response.json
```

2. **Query de la base de datos:**

```sql
SELECT * FROM menu WHERE active = true;
```

3. **Código del MenuService/MenuController** (Java)

---

## 🎯 Recomendación Final

**Ve con la Opción 1:** Backend envía TODO, Frontend filtra.

**Razones:**

- ✅ Más simple en backend
- ✅ Ya tienes `MenuFilterService` implementado en frontend
- ✅ Ya tienes `/api/permission/validate-feature` funcionando
- ✅ Más flexible para cambios futuros
- ✅ Mejor UX (filtrado instantáneo en frontend)

**Cambio necesario:** Una línea en el backend:

```java
// De:
return menuService.findByUser(userId);
// A:
return menuService.findAll();
```

¡Eso es todo! 🚀
