# 🔧 Script SQL - Agregar Feature Code a Menús

## 📋 Preparación del Backend

Este script prepara la base de datos para enviar `featureCode` desde el backend, eliminando la necesidad de hacer deploy del frontend cada vez que se agrega un menú.

---

## 🗄️ Script SQL Completo

```sql
-- ==========================================
-- 1. AGREGAR COLUMNA feature_code
-- ==========================================

-- Agregar columna a la tabla menu
ALTER TABLE menu
ADD COLUMN IF NOT EXISTS feature_code VARCHAR(50);

-- Crear índice para mejor performance
CREATE INDEX IF NOT EXISTS idx_menu_feature_code
ON menu(feature_code);

-- ==========================================
-- 2. ACTUALIZAR MENÚS EXISTENTES
-- ==========================================

-- Facturación
UPDATE menu
SET feature_code = 'INVOICE_CREATE'
WHERE url = '/invoices' OR url LIKE '%/invoices%';

-- Notas de Crédito
UPDATE menu
SET feature_code = 'CREDIT_NOTE_CREATE'
WHERE url = '/credit-notes' OR url LIKE '%/credit-notes%';

-- Clientes
UPDATE menu
SET feature_code = 'CLIENT_CREATE'
WHERE url = '/clients' OR url LIKE '%/clients%';

-- Usuarios
UPDATE menu
SET feature_code = 'USER_MANAGEMENT'
WHERE url = '/users' OR url LIKE '%/users%';

-- Compañías
UPDATE menu
SET feature_code = 'COMPANY_SETTINGS'
WHERE url = '/companies' OR url LIKE '%/companies%';

-- Establecimientos
UPDATE menu
SET feature_code = 'ESTABLISHMENT_MANAGEMENT'
WHERE url = '/establishments' OR url LIKE '%/establishments%';

-- Puntos de Emisión
UPDATE menu
SET feature_code = 'ESTABLISHMENT_MANAGEMENT'
WHERE url = '/invoice-points' OR url LIKE '%/invoice-points%';

-- Reportes Avanzados
UPDATE menu
SET feature_code = 'REPORT_ADVANCED'
WHERE url IN ('/reports/sales-dashboard', '/reports/branch-comparison')
   OR url LIKE '%sales-dashboard%'
   OR url LIKE '%branch-comparison%';

-- Productos
UPDATE menu
SET feature_code = 'INVOICE_CREATE'  -- Productos suele estar asociado a facturación
WHERE url = '/products' OR url LIKE '%/products%';

-- ==========================================
-- 3. VERIFICAR RESULTADOS
-- ==========================================

-- Ver todos los menús con sus feature codes
SELECT
  id,
  name,
  url,
  feature_code,
  active,
  parent_id
FROM menu
WHERE active = true
ORDER BY
  CASE WHEN parent_id IS NULL THEN id ELSE parent_id END,
  parent_id NULLS FIRST,
  id;

-- Ver menús sin feature code (deberían ser solo menús padre o públicos)
SELECT
  id,
  name,
  url,
  feature_code,
  parent_id
FROM menu
WHERE active = true
  AND feature_code IS NULL
ORDER BY id;

-- Contar menús por feature code
SELECT
  feature_code,
  COUNT(*) as total_menus,
  STRING_AGG(name, ', ') as menu_names
FROM menu
WHERE active = true
  AND feature_code IS NOT NULL
GROUP BY feature_code
ORDER BY total_menus DESC;

-- ==========================================
-- 4. VERIFICAR FEATURES EXISTEN
-- ==========================================

-- Ver si todos los feature_codes tienen su registro en la tabla features
SELECT
  m.feature_code,
  COUNT(m.id) as menu_count,
  CASE
    WHEN f.feature_code IS NULL THEN '❌ No existe en features'
    ELSE '✅ Existe'
  END as status
FROM menu m
LEFT JOIN features f ON m.feature_code = f.feature_code
WHERE m.active = true
  AND m.feature_code IS NOT NULL
GROUP BY m.feature_code, f.feature_code
ORDER BY status, m.feature_code;

-- ==========================================
-- 5. EJEMPLO: AGREGAR NUEVO MENÚ CON FEATURE CODE
-- ==========================================

-- Ejemplo de cómo agregar un menú nuevo con feature code
-- (Ejecutar solo si quieres agregar este menú de prueba)

/*
-- Agregar menú nuevo: Reportes Financieros
INSERT INTO menu (
  name,
  icon,
  url,
  feature_code,
  parent_id,
  active,
  order_num,
  created_at
)
VALUES (
  'Reportes Financieros',  -- nombre del menú
  'account_balance',        -- ícono de Material Icons
  '/reports/financial',     -- URL del menú
  'REPORT_FINANCIAL',       -- feature code
  (SELECT id FROM menu WHERE name = 'Reportes' AND parent_id IS NULL),  -- parent_id (grupo Reportes)
  true,                     -- activo
  10,                       -- orden
  NOW()
);

-- Verificar que se agregó
SELECT * FROM menu WHERE url = '/reports/financial';

-- Asegurar que el feature code existe en la tabla features
INSERT INTO features (
  feature_code,
  feature_name,
  description,
  feature_module,
  allowed_roles,
  required_plans,
  is_active,
  created_at
)
VALUES (
  'REPORT_FINANCIAL',
  'Reportes Financieros',
  'Acceso a reportes financieros avanzados y estados de cuenta',
  'REPORTS',
  ARRAY[1, 2],  -- roles: ADMIN, ACCOUNTANT
  ARRAY['PROFESSIONAL', 'ENTERPRISE'],
  true,
  NOW()
)
ON CONFLICT (feature_code) DO NOTHING;
*/

-- ==========================================
-- 6. LIMPIEZA (Opcional - Solo si quieres eliminar un menú)
-- ==========================================

/*
-- Desactivar un menú en lugar de eliminarlo
UPDATE menu
SET active = false
WHERE url = '/reports/financial';

-- Eliminar feature code de un menú
UPDATE menu
SET feature_code = NULL
WHERE url = '/algun-menu';
*/

-- ==========================================
-- 7. VALIDACIÓN FINAL
-- ==========================================

-- Esta query debe mostrar la estructura completa con feature codes
SELECT
  COALESCE(parent.name, child.name) as grupo,
  child.name as submenu,
  child.url,
  child.feature_code,
  child.active,
  CASE
    WHEN child.feature_code IS NULL THEN '⚠️  Sin feature code'
    WHEN f.feature_code IS NOT NULL THEN '✅ Feature válido'
    ELSE '❌ Feature no existe'
  END as validation
FROM menu child
LEFT JOIN menu parent ON child.parent_id = parent.id
LEFT JOIN features f ON child.feature_code = f.feature_code
WHERE child.active = true
ORDER BY
  COALESCE(parent.id, child.id),
  child.parent_id NULLS FIRST,
  child.order_num,
  child.id;
```

---

## 🎯 Resultado Esperado

Después de ejecutar este script, deberías ver algo como:

```
┌────────────────────────┬──────────────────────────┬────────────────────────────┬───────────────────────────┐
│ grupo                  │ submenu                  │ url                        │ feature_code              │
├────────────────────────┼──────────────────────────┼────────────────────────────┼───────────────────────────┤
│ Facturación            │ Nueva Factura            │ /invoices                  │ INVOICE_CREATE            │
│ Facturación            │ Lista de Facturas        │ /invoices/list             │ INVOICE_CREATE            │
│ Notas de Crédito       │ Nueva Nota de Crédito    │ /credit-notes              │ CREDIT_NOTE_CREATE        │
│ Clientes               │ Lista de Clientes        │ /clients                   │ CLIENT_CREATE             │
│ Productos              │ Lista de Productos       │ /products                  │ INVOICE_CREATE            │
│ Reportes               │ Ventas por Período       │ /reports/sales             │ NULL                      │
│ Reportes               │ Dashboard Avanzado       │ /reports/sales-dashboard   │ REPORT_ADVANCED           │
│ Reportes               │ Comparativa Sucursales   │ /reports/branch-comparison │ REPORT_ADVANCED           │
│ Usuarios               │ Gestión de Usuarios      │ /users                     │ USER_MANAGEMENT           │
│ Establecimientos       │ Lista Establecimientos   │ /establishments            │ ESTABLISHMENT_MANAGEMENT  │
│ Establecimientos       │ Puntos de Emisión        │ /invoice-points            │ ESTABLISHMENT_MANAGEMENT  │
│ Compañías              │ Información Compañía     │ /companies                 │ COMPANY_SETTINGS          │
└────────────────────────┴──────────────────────────┴────────────────────────────┴───────────────────────────┘
```

---

## 🔧 Backend - Actualizar Modelo Java

Después de ejecutar el SQL, actualiza tu modelo en Java:

```java
// Menu.java
package com.ebilling.authentication.model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "menu")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "icon")
    private String icon;

    @Column(name = "url")
    private String url;

    // 👇 AGREGAR ESTE CAMPO
    @Column(name = "feature_code")
    private String featureCode;

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
    private List<Menu> subMenus;

    // Getters y Setters

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    // ... otros getters/setters
}
```

---

## ✅ Verificación

### 1. En Base de Datos

```sql
-- Debe mostrar feature_code poblado
SELECT id, name, url, feature_code
FROM menu
WHERE active = true
  AND url IS NOT NULL
  AND url != 'null'
ORDER BY id;
```

### 2. En Backend (curl)

```bash
# Debe incluir "featureCode" en el response
curl -X GET http://localhost:8280/api/menu/0102030405 \
  -H "Authorization: Bearer <token>" \
  | jq '.[].subMenus[] | {name, url, featureCode}'
```

**Response esperado:**

```json
{
  "name": "Nueva Factura",
  "url": "/invoices",
  "featureCode": "INVOICE_CREATE"  // 👈 Debe aparecer
}
{
  "name": "Nueva Nota de Crédito",
  "url": "/credit-notes",
  "featureCode": "CREDIT_NOTE_CREATE"  // 👈 Debe aparecer
}
```

### 3. En Frontend (Console)

Una vez que el backend devuelva `featureCode`, verás en los logs:

```
📋 [MenuFilter] Estructura: (7) [{…}]
  0: {id: 1, name: "Facturación", subMenus: Array(2)}
    subMenus:
      0: {id: 10, name: "Nueva Factura", url: "/invoices", featureCode: "INVOICE_CREATE"}  // 👈
      1: {id: 11, name: "Lista", url: "/invoices/list", featureCode: "INVOICE_CREATE"}    // 👈
```

---

## 🎉 Beneficios

Una vez implementado:

✅ **Agregar menús sin deploy** - Solo SQL
✅ **Gestión centralizada** - Todo en DB
✅ **Más escalable** - Crece fácilmente
✅ **Sin código duplicado** - Un solo lugar

**Ejemplo de agregar menú nuevo:**

```sql
-- Solo ejecutar este SQL:
INSERT INTO menu (name, icon, url, feature_code, parent_id, active, order_num)
VALUES ('API Webhooks', 'webhook', '/settings/webhooks', 'WEBHOOK_CONFIG', 8, true, 5);

-- ¡Listo! El menú aparece automáticamente (si el usuario tiene permiso)
-- NO se necesita deploy del frontend
```

---

## 📞 Siguiente Paso

1. **Ejecuta el script SQL** en tu base de datos
2. **Actualiza el modelo Java** (agregar campo `featureCode`)
3. **Reinicia el backend**
4. **Recarga el frontend** (sin deploy, solo Cmd+R)
5. **Verifica** que los menús se filtran correctamente

¿Alguna duda sobre el script? 🚀
