# 🐛 Checklist de Debug: Menú Solo Muestra "Compañía"

## 🎯 Problema Actual

**Síntoma:** El menú lateral solo muestra "Compañía" (sin submenús).

**Screenshot:** Se ve en la aplicación que solo hay 1 grupo de menú.

---

## ✅ Pasos de Verificación

### Paso 1: Verificar Console Logs

**Abrir DevTools:**

```
Cmd+Option+I (Mac) o F12 (Windows)
→ Pestaña Console
```

**Buscar estos logs:**

```javascript
📋 [MenuFilter] Menús recibidos del backend: X grupos  // ← ¿Cuántos grupos?
📋 [MenuFilter] Estructura: (X) [{…}]                  // ← Expandir esto
```

**Clic en `(X) [{…}]` para expandir y ver:**

```javascript
0: {
  id: 3,
  name: "Compañía",
  icon: "store",
  url: "null",
  subMenus: []  // ← ¿Está vacío?
}
```

---

### Paso 2: Verificar Network Tab

**En DevTools:**

```
→ Pestaña Network
→ Buscar: menu
→ Clic en la petición GET /api/menu/...
→ Ver Response
```

**Debe mostrar algo como:**

```json
[
  {
    "id": 3,
    "name": "Compañía",
    "subMenus": [] // ❌ VACÍO
  }
]
```

**O debería mostrar (correcto):**

```json
[
  {
    "id": 1,
    "name": "Facturas",
    "subMenus": [
      {
        "name": "Nueva Factura",
        "url": "/billing",
        "featureCode": "INVOICE_CREATE"
      }
    ]
  },
  {
    "id": 2,
    "name": "Clientes",
    "subMenus": [...]
  },
  // ... más grupos
]
```

---

### Paso 3: Verificar en Base de Datos

**Ejecutar en PostgreSQL:**

```sql
-- 1. Ver TODOS los menús activos
SELECT
  id,
  name,
  url,
  icon,
  feature_code,
  parent_id,
  active
FROM menu
WHERE active = true
ORDER BY
  CASE WHEN parent_id IS NULL THEN id ELSE parent_id END,
  parent_id NULLS FIRST,
  id;

-- Debe mostrar al menos 15-20 menús
-- Si solo muestra 1 o muy pocos → Problema en la base de datos
```

```sql
-- 2. Ver jerarquía de menús
SELECT
  COALESCE(parent.name, child.name) as grupo,
  child.name as submenu,
  child.url,
  child.feature_code,
  child.active
FROM menu child
LEFT JOIN menu parent ON child.parent_id = parent.id
WHERE child.active = true
ORDER BY
  COALESCE(parent.id, child.id),
  child.parent_id NULLS FIRST,
  child.id;

-- Debe mostrar estructura completa:
-- grupo          | submenu                  | url           | feature_code
-- Facturación    | Nueva Factura            | /billing      | INVOICE_CREATE
-- Facturación    | Lista de Facturas        | /invoices     | INVOICE_CREATE
-- Clientes       | Gestionar Clientes       | /customer     | CLIENT_CREATE
-- etc...
```

```sql
-- 3. Verificar menús padre (grupos)
SELECT
  id,
  name,
  icon,
  active,
  (SELECT COUNT(*) FROM menu sub WHERE sub.parent_id = menu.id) as num_submenus
FROM menu
WHERE parent_id IS NULL
  AND active = true
ORDER BY id;

-- Debe mostrar varios grupos:
-- id | name           | icon  | active | num_submenus
-- 1  | Facturación    | store | true   | 2
-- 2  | Clientes       | people| true   | 1
-- 3  | Compañía       | store | true   | 1
-- ...
```

---

### Paso 4: Verificar Usuario y Roles

```sql
-- Ver roles del usuario actual
SELECT
  u.identification,
  u.name,
  r.code_role,
  ur.id_role
FROM "user" u
JOIN user_role ur ON u.id = ur.id_user
JOIN role r ON ur.id_role = r.id
WHERE u.identification = '0102030405';  -- Tu usuario

-- Resultado esperado:
-- identification | name          | code_role | id_role
-- 0102030405    | Eddie Hidalgo | admin     | 1
```

```sql
-- Ver qué menús puede ver este rol
SELECT DISTINCT
  m.id,
  m.name,
  mr.role_id
FROM menu m
JOIN menu_role mr ON m.id = mr.menu_id
WHERE mr.role_id = 1  -- 1 = admin
  AND m.active = true
ORDER BY m.id;

-- Debe mostrar MUCHOS menús (15-20)
-- Si solo muestra 1 → El problema está en menu_role
```

---

## 🎯 Diagnósticos Posibles

### Diagnóstico A: Backend Filtra Antes de Enviar ❌

**Síntoma:**

- Console: `Menús recibidos del backend: 1 grupos`
- Network: Solo 1 menú en el response
- DB: Muchos menús existen

**Causa:** El backend filtra por rol ANTES de enviar.

**Solución:** Backend debe enviar TODOS los menús.

```java
// En MenuServiceImpl.java
public List<MenuDto> getMenusByUsername(String username) {
    // ❌ NO filtrar por rol aquí
    // return menuRepository.findByUserRoles(...);

    // ✅ Enviar todos
    return menuRepository.findAllActiveMenus();
}
```

---

### Diagnóstico B: Pocos Menús en Base de Datos ❌

**Síntoma:**

- Console: `Menús recibidos del backend: 1 grupos`
- DB: Solo 1-2 menús existen
- Falta insertar datos

**Causa:** La base de datos no tiene los menús poblados.

**Solución:** Ejecutar script SQL para poblar menús.

```sql
-- Ver archivo: scripts/populate-menus.sql
-- O insertar manualmente los menús básicos
```

---

### Diagnóstico C: Usuario Sin Rol en menu_role ❌

**Síntoma:**

- Console: `Menús recibidos del backend: 1 grupos`
- DB: Muchos menús existen
- Query menu_role para el rol: Solo 1 menú

**Causa:** La tabla `menu_role` no tiene las asociaciones correctas.

**Solución:** Agregar relaciones menú-rol.

```sql
-- Verificar
SELECT menu_id, role_id FROM menu_role WHERE role_id = 1;

-- Si está vacío, poblar:
INSERT INTO menu_role (menu_id, role_id)
SELECT id, 1 FROM menu WHERE active = true;  -- Asignar todos los menús al admin
```

---

### Diagnóstico D: Frontend Filtra Demasiado ❌

**Síntoma:**

- Console: `Menús recibidos del backend: 8 grupos`
- Console: `Menús después de filtrar: 1 grupos`
- El filtrado está demasiado restrictivo

**Causa:** El usuario no tiene plan o el plan no permite nada.

**Solución:** Verificar suscripción.

```sql
-- Ver suscripción del usuario
SELECT
  s.plan,
  s.status,
  s.start_date,
  s.end_date
FROM subscription s
JOIN "user" u ON s.company_id = u.company_id
WHERE u.identification = '0102030405';

-- Debe tener plan activo:
-- plan   | status | start_date | end_date
-- FREE   | ACTIVE | 2025-01-01 | 2026-01-01
```

---

## 📋 Checklist de Validación

Marca lo que encuentres:

### Console (DevTools)

- [ ] `📋 [MenuFilter] Menús recibidos del backend: X grupos` → X = ?
- [ ] `✅ [MenuFilter] Menús después de filtrar: Y grupos` → Y = ?
- [ ] ¿Hay errors en la console?

### Network (DevTools)

- [ ] Request a `/api/menu/{id}` se hizo correctamente
- [ ] Response status: 200 OK
- [ ] Response tiene 1 solo menú o tiene varios?
- [ ] Los menús tienen `subMenus` poblados?

### Base de Datos

- [ ] Query 1: ¿Cuántos menús activos hay? → Total: \_\_\_
- [ ] Query 2: ¿La jerarquía se ve correcta? → Sí / No
- [ ] Query 3: ¿Cuántos grupos padre hay? → Total: \_\_\_
- [ ] Query 4: ¿El usuario tiene roles asignados? → Sí / No
- [ ] Query 5: ¿El rol tiene menús en menu_role? → Total: \_\_\_

---

## 🚀 Siguientes Pasos

Una vez que completes el checklist, pega aquí:

1. **Console logs:**

   ```
   [Pegar aquí los logs de console]
   ```

2. **Network response:**

   ```json
   [Pegar aquí el JSON del response de /api/menu]
   ```

3. **Resultados de queries SQL:**
   ```
   [Pegar aquí los resultados de las queries]
   ```

Con esa información puedo darte la solución exacta. 🎯
