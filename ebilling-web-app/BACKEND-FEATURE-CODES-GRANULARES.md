# 🔐 Requerimiento Backend: Feature Codes Granulares por Menú Hoja

## 📋 Problema Actual

Actualmente, los `feature_code` están asignados a menús padre o intermedios, lo que causa que múltiples menús hoja compartan el mismo código genérico. Esto impide tener granularidad en los permisos.

### Ejemplo del Problema:

```
Reportes (sin feature_code) ✅
├── Reportes operativos (feature_code: REPORT_OPERATIONAL) ❌ PROBLEMA
│   ├── Ventas por periodo (sin feature_code) ❌
│   ├── Ventas por cliente (sin feature_code) ❌
│   ├── Ventas por producto (sin feature_code) ❌
│   └── Reporte de notas de crédito (sin feature_code) ❌
├── Reportes comerciales (feature_code: REPORT_SALES) ❌ PROBLEMA
│   ├── Dashboard de ventas (sin feature_code) ❌
│   ├── Comparativo por sucursal (sin feature_code) ❌
│   └── Reporte de formas de pago (sin feature_code) ❌
```

**Resultado:** Todos los reportes operativos comparten `REPORT_OPERATIONAL`, no se pueden gestionar permisos individuales.

---

## ✅ Solución Requerida

Cada **menú hoja** (menú sin submenús) debe tener su propio `feature_code` único. Los menús padre/intermedios NO deben tener `feature_code`.

### Estructura Correcta:

```
Reportes (sin feature_code) ✅
├── Reportes operativos (sin feature_code) ✅
│   ├── Ventas por periodo (feature_code: REPORT_SALES_BY_PERIOD) ✅
│   ├── Ventas por cliente (feature_code: REPORT_SALES_BY_CLIENT) ✅
│   ├── Ventas por producto (feature_code: REPORT_SALES_BY_PRODUCT) ✅
│   └── Reporte de notas de crédito (feature_code: REPORT_CREDIT_NOTES) ✅
├── Reportes comerciales (sin feature_code) ✅
│   ├── Dashboard de ventas (feature_code: REPORT_SALES_DASHBOARD) ✅
│   ├── Comparativo por sucursal (feature_code: REPORT_BRANCH_COMPARISON) ✅
│   └── Reporte de formas de pago (feature_code: REPORT_PAYMENT_METHODS) ✅
```

---

## 🗄️ Script SQL para Actualizar Base de Datos

### Paso 1: Verificar Estructura Actual

```sql
-- Ver todos los menús con sus feature codes actuales
SELECT
    id,
    name,
    url,
    feature_code,
    parent_id_menu,
    (SELECT COUNT(*) FROM menu m2 WHERE m2.parent_id_menu = m.id) as submenus_count
FROM menu m
WHERE active = true
ORDER BY parent_id_menu NULLS FIRST, id;
```

### Paso 2: Limpiar Feature Codes de Menús Padre/Intermedios

```sql
-- Remover feature_code de menús que tienen submenús (son padres/intermedios)
UPDATE menu
SET feature_code = NULL
WHERE id IN (
    SELECT DISTINCT parent_id_menu
    FROM menu
    WHERE parent_id_menu IS NOT NULL
)
AND active = true;

-- Verificar que se removieron correctamente
SELECT id, name, feature_code,
       (SELECT COUNT(*) FROM menu m2 WHERE m2.parent_id_menu = m.id) as submenus_count
FROM menu m
WHERE active = true
ORDER BY id;
```

### Paso 3: Asignar Feature Codes Únicos a Menús Hoja

```sql
-- ============================================================
-- REPORTES OPERATIVOS
-- ============================================================

-- Ventas por periodo
UPDATE menu
SET feature_code = 'REPORT_SALES_BY_PERIOD'
WHERE url = 'sales-by-period'
  AND active = true;

-- Ventas por cliente
UPDATE menu
SET feature_code = 'REPORT_SALES_BY_CLIENT'
WHERE url = 'sales-by-client'
  AND active = true;

-- Ventas por producto
UPDATE menu
SET feature_code = 'REPORT_SALES_BY_PRODUCT'
WHERE url = 'sales-by-product'
  AND active = true;

-- Reporte de notas de crédito
UPDATE menu
SET feature_code = 'REPORT_CREDIT_NOTES'
WHERE url = 'credit-notes'
  AND active = true;

-- Reporte de facturas pendientes de envío
UPDATE menu
SET feature_code = 'REPORT_PENDING_INVOICES'
WHERE url = 'pending-invoices'
  AND active = true;

-- ============================================================
-- REPORTES COMERCIALES / AVANZADOS
-- ============================================================

-- Dashboard de ventas
UPDATE menu
SET feature_code = 'REPORT_SALES_DASHBOARD'
WHERE url = 'sales-dashboard'
  AND active = true;

-- Comparativo por sucursal / punto de emisión
UPDATE menu
SET feature_code = 'REPORT_BRANCH_COMPARISON'
WHERE url = 'branch-comparison'
  AND active = true;

-- Reporte de formas de pago
UPDATE menu
SET feature_code = 'REPORT_PAYMENT_METHODS'
WHERE url = 'payment-methods'
  AND active = true;

-- ============================================================
-- FACTURACIÓN
-- ============================================================

-- Facturas (ya debería tener INVOICE_CREATE, verificar)
UPDATE menu
SET feature_code = 'INVOICE_CREATE'
WHERE url = '/invoices'
  AND active = true
  AND (feature_code IS NULL OR feature_code != 'INVOICE_CREATE');

-- Nota de crédito (ya debería tener CREDIT_NOTE_CREATE, verificar)
UPDATE menu
SET feature_code = 'CREDIT_NOTE_CREATE'
WHERE url = '/credit-note'
  AND active = true
  AND (feature_code IS NULL OR feature_code != 'CREDIT_NOTE_CREATE');

-- ============================================================
-- CLIENTES
-- ============================================================

-- Gestionar clientes
UPDATE menu
SET feature_code = 'CLIENT_CREATE'
WHERE url = '/clients'
  AND active = true
  AND (feature_code IS NULL OR feature_code != 'CLIENT_CREATE');

-- ============================================================
-- PRODUCTOS
-- ============================================================

-- Gestionar productos
UPDATE menu
SET feature_code = 'PRODUCT_MANAGEMENT'
WHERE url = '/products'
  AND active = true
  AND (feature_code IS NULL OR feature_code != 'PRODUCT_MANAGEMENT');

-- ============================================================
-- ADMINISTRACIÓN
-- ============================================================

-- Gestionar compañía
UPDATE menu
SET feature_code = 'COMPANY_SETTINGS'
WHERE url = '/companies'
  AND active = true
  AND (feature_code IS NULL OR feature_code != 'COMPANY_SETTINGS');

-- Gestionar sucursal
UPDATE menu
SET feature_code = 'ESTABLISHMENT_MANAGEMENT'
WHERE url = '/establishments'
  AND active = true
  AND (feature_code IS NULL OR feature_code != 'ESTABLISHMENT_MANAGEMENT');

-- Gestionar Punto de Emisión
UPDATE menu
SET feature_code = 'ESTABLISHMENT_MANAGEMENT'
WHERE url = '/invoice-points'
  AND active = true
  AND (feature_code IS NULL OR feature_code != 'ESTABLISHMENT_MANAGEMENT');

-- Gestionar usuarios por compañía
UPDATE menu
SET feature_code = 'USER_MANAGEMENT'
WHERE url = '/company-users'
  AND active = true
  AND (feature_code IS NULL OR feature_code != 'USER_MANAGEMENT');

-- Gestión de Permisos (Super Admin)
UPDATE menu
SET feature_code = 'ROLE_PERMISSION_MANAGEMENT'
WHERE url = '/admin/role-permissions'
  AND active = true
  AND (feature_code IS NULL OR feature_code != 'ROLE_PERMISSION_MANAGEMENT');

-- Gestión de Suscripciones (Super Admin)
UPDATE menu
SET feature_code = 'SUBSCRIPTION_MANAGEMENT'
WHERE url = '/admin/subscriptions'
  AND active = true
  AND (feature_code IS NULL OR feature_code != 'SUBSCRIPTION_MANAGEMENT');
```

### Paso 4: Verificar Resultado Final

```sql
-- Ver todos los menús hoja (sin submenús) con sus feature codes
SELECT
    m.id,
    m.name,
    m.url,
    m.feature_code,
    (SELECT name FROM menu WHERE id = m.parent_id_menu) as parent_name,
    (SELECT COUNT(*) FROM menu m2 WHERE m2.parent_id_menu = m.id) as submenus_count
FROM menu m
WHERE active = true
  AND (SELECT COUNT(*) FROM menu m2 WHERE m2.parent_id_menu = m.id) = 0  -- Solo menús hoja
ORDER BY m.parent_id_menu, m.id;

-- Verificar que ningún menú padre/intermedio tenga feature_code
SELECT
    m.id,
    m.name,
    m.url,
    m.feature_code,
    (SELECT COUNT(*) FROM menu m2 WHERE m2.parent_id_menu = m.id) as submenus_count
FROM menu m
WHERE active = true
  AND (SELECT COUNT(*) FROM menu m2 WHERE m2.parent_id_menu = m.id) > 0  -- Solo menús con submenús
  AND m.feature_code IS NOT NULL;  -- Debería estar vacío

-- Verificar feature codes duplicados (no debería haber)
SELECT
    feature_code,
    COUNT(*) as count,
    STRING_AGG(name, ', ') as menu_names
FROM menu
WHERE active = true
  AND feature_code IS NOT NULL
GROUP BY feature_code
HAVING COUNT(*) > 1;
```

---

## 📝 Convención de Nombres para Feature Codes

### Estructura: `MODULO_ACCION_DETALLE`

**Reportes:**

- `REPORT_SALES_BY_PERIOD` - Ventas por periodo
- `REPORT_SALES_BY_CLIENT` - Ventas por cliente
- `REPORT_SALES_BY_PRODUCT` - Ventas por producto
- `REPORT_CREDIT_NOTES` - Reporte de notas de crédito
- `REPORT_PENDING_INVOICES` - Reporte de facturas pendientes
- `REPORT_SALES_DASHBOARD` - Dashboard de ventas
- `REPORT_BRANCH_COMPARISON` - Comparativo por sucursal
- `REPORT_PAYMENT_METHODS` - Reporte de formas de pago

**Facturación:**

- `INVOICE_CREATE` - Crear facturas
- `CREDIT_NOTE_CREATE` - Crear notas de crédito

**Clientes:**

- `CLIENT_CREATE` - Gestionar clientes

**Productos:**

- `PRODUCT_MANAGEMENT` - Gestionar productos

**Administración:**

- `COMPANY_SETTINGS` - Configuración de compañía
- `ESTABLISHMENT_MANAGEMENT` - Gestión de sucursales/puntos
- `USER_MANAGEMENT` - Gestión de usuarios
- `ROLE_PERMISSION_MANAGEMENT` - Gestión de permisos (Super Admin)
- `SUBSCRIPTION_MANAGEMENT` - Gestión de suscripciones (Super Admin)

---

## ✅ Checklist de Validación

- [ ] Ningún menú padre/intermedio tiene `feature_code`
- [ ] Todos los menús hoja tienen `feature_code` único
- [ ] No hay `feature_code` duplicados
- [ ] Los `feature_code` siguen la convención de nombres
- [ ] El endpoint `/api/menu/{userName}` retorna `feature_code` en cada menú hoja
- [ ] El frontend puede extraer features individuales de cada menú hoja

---

## 🔄 Rollback (Si es necesario)

```sql
-- Restaurar feature codes genéricos (solo si es necesario)
UPDATE menu
SET feature_code = 'REPORT_OPERATIONAL'
WHERE url IN ('sales-by-period', 'sales-by-client', 'sales-by-product', 'credit-notes', 'pending-invoices')
  AND active = true;

UPDATE menu
SET feature_code = 'REPORT_ADVANCED'
WHERE url IN ('sales-dashboard', 'branch-comparison')
  AND active = true;

UPDATE menu
SET feature_code = 'REPORT_SALES'
WHERE url = 'payment-methods'
  AND active = true;
```

---

## 📧 Notas para el Backend

1. **Validación en el Backend:** El endpoint `/api/menu/{userName}` debe asegurarse de que:

   - Solo los menús hoja (sin `subMenus` o con `subMenus.length === 0`) tengan `feature_code`
   - Los menús padre/intermedios NO deben incluir `feature_code` en la respuesta

2. **Migración de Permisos Existentes:**

   - Si hay permisos asignados a `REPORT_OPERATIONAL`, `REPORT_SALES`, `REPORT_ADVANCED` genéricos, estos deben migrarse a los nuevos feature codes específicos
   - O mantener ambos (genéricos y específicos) durante un período de transición

3. **Compatibilidad:**
   - El frontend ya está preparado para manejar feature codes únicos por menú hoja
   - Si un menú hoja no tiene `feature_code`, el frontend generará uno automáticamente basado en la URL

---

**Fecha de Creación:** 2025-01-XX  
**Prioridad:** Alta  
**Impacto:** Permite granularidad completa en permisos por menú individual
