# 🏗️ Arquitectura de Permisos de Menús - Bottom-Up

## 📋 Principio Fundamental

**Los menús hoja (hijos finales) controlan los permisos. Los menús padre se muestran automáticamente si tienen al menos un hijo visible.**

---

## 🎯 Reglas de Validación

### 1. **Menús Hoja (Sin Submenús)**

- ✅ **DEBEN tener `feature_code`**
- ✅ **Se validan con `canAccess(featureCode)`**
- ✅ **Si tienen permiso → se muestran**
- ✅ **Si NO tienen permiso → se ocultan**

**Ejemplo:**

```typescript
{
  id: 22,
  name: "Ventas por periodo",
  url: "sales-by-period",
  featureCode: "REPORT_SALES_BY_PERIOD",  // ✅ Tiene feature_code
  subMenus: []  // ✅ Es hoja (sin submenús)
}
```

### 2. **Menús Padre (Con Submenús)**

- ✅ **Pueden tener `feature_code`** (para referencia/documentación, pero **NO se valida**)
- ✅ **Se muestran automáticamente si tienen al menos un hijo visible**
- ✅ **Si todos los hijos están ocultos → el padre también se oculta**
- ⚠️ **El `feature_code` del padre se ignora en la validación** (solo se valida en hojas)

**Ejemplo:**

```typescript
{
  id: 21,
  name: "Reportes operativos",
  url: "/reports/operational",
  featureCode: "REPORT_OPERATIONAL",  // ✅ Puede tenerlo (para referencia), pero NO se valida
  subMenus: [
    { id: 22, featureCode: "REPORT_SALES_BY_PERIOD", ... },  // ✅ Este SÍ se valida
    { id: 23, featureCode: "REPORT_SALES_BY_CLIENT", ... }  // ✅ Este SÍ se valida
  ]
}
```

### 3. **Validación Bottom-Up (De Abajo Hacia Arriba)**

```
1. Validar todos los menús hoja (nivel más bajo)
   ├─ Si tiene permiso → visible
   └─ Si NO tiene permiso → oculto

2. Para cada menú padre:
   ├─ Si tiene al menos 1 hijo visible → mostrar padre
   └─ Si todos los hijos están ocultos → ocultar padre

3. Repetir recursivamente hacia arriba
```

---

## 📊 Ejemplo Práctico

### Estructura de Menús:

```
Reportes (padre, sin feature_code)
├── Reportes operativos (padre intermedio, sin feature_code)
│   ├── Ventas por periodo (hoja, REPORT_SALES_BY_PERIOD) ✅ PERMITIDO
│   ├── Ventas por cliente (hoja, REPORT_SALES_BY_CLIENT) ❌ DENEGADO
│   └── Ventas por producto (hoja, REPORT_SALES_BY_PRODUCT) ❌ DENEGADO
└── Reportes comerciales (padre intermedio, sin feature_code)
    ├── Dashboard de ventas (hoja, REPORT_SALES_DASHBOARD) ❌ DENEGADO
    └── Comparativo por sucursal (hoja, REPORT_BRANCH_COMPARISON) ❌ DENEGADO
```

### Resultado de Validación:

```
Reportes ✅ (visible porque tiene al menos un hijo visible)
├── Reportes operativos ✅ (visible porque tiene "Ventas por periodo" visible)
│   └── Ventas por periodo ✅ (visible - tiene permiso)
└── Reportes comerciales ❌ (oculto - todos sus hijos están ocultos)
```

---

## 🔧 Implementación en Frontend

### Lógica Actual (Ya Implementada)

El método `filterSubMenus()` ya implementa esta lógica:

```typescript
private filterSubMenus(menu: Menu): Observable<Menu | null> {
  if (!menu.subMenus || menu.subMenus.length === 0) {
    return of(menu);
  }

  // 1. Filtrar todos los hijos (validar feature_code de cada hijo)
  const filteredSubMenus$ = menu.subMenus.map((subMenu) =>
    this.filterMenu(subMenu)
  );

  return forkJoin(filteredSubMenus$).pipe(
    map((filteredSubMenus) => {
      const validSubMenus = filteredSubMenus.filter(
        (subMenu) => subMenu !== null
      ) as Menu[];

      // 2. Si después de filtrar no quedan submenús, ocultar el grupo completo
      if (validSubMenus.length === 0) {
        return null;  // ❌ Ocultar padre si no tiene hijos visibles
      }

      // 3. Retornar el menú con los submenús filtrados
      return {
        ...menu,
        subMenus: validSubMenus,  // ✅ Mostrar padre con solo hijos visibles
      };
    })
  );
}
```

### Mejora Necesaria

Modificar `filterMenu()` para que **NO valide `feature_code` en menús padre**:

```typescript
private filterMenu(menu: Menu): Observable<Menu | null> {
  // Si tiene submenús, filtrarlos recursivamente (NO validar feature_code del padre)
  if (menu.subMenus && menu.subMenus.length > 0) {
    return this.filterSubMenus(menu);  // ✅ Ya implementado correctamente
  }

  // Si es un item final (hoja), verificar permisos
  const featureCode = menu.featureCode || this.getFeatureCodeFromUrl(menu.url);

  if (!featureCode) {
    // Si no tiene feature code, permitir (es una ruta pública)
    return of(menu);
  }

  // Validar permiso del menú hoja
  return this.permissionService.canAccess(featureCode).pipe(
    map((allowed) => {
      return allowed ? menu : null;
    })
  );
}
```

---

## 🗄️ Estructura en Base de Datos

### Reglas para `feature_code` en la tabla `menu`:

```sql
-- ✅ CORRECTO: Menús hoja DEBEN tener feature_code (se valida)
UPDATE menu
SET feature_code = 'REPORT_SALES_BY_PERIOD'
WHERE id_menu = 22  -- Ventas por periodo (hoja)
  AND (SELECT COUNT(*) FROM menu WHERE parent_id_menu = 22) = 0;  -- Sin hijos

-- ✅ OPCIONAL: Menús padre PUEDEN tener feature_code (NO se valida, solo referencia)
-- No es necesario removerlo, el código lo ignora automáticamente
```

### Query para Verificar Estructura:

```sql
-- Verificar que los menús hoja tienen feature_code (los padres pueden tenerlo o no)
SELECT
    m.id_menu,
    m.name,
    m.url,
    m.feature_code,
    (SELECT COUNT(*) FROM menu m2 WHERE m2.parent_id_menu = m.id_menu) as hijos_count,
    CASE
        WHEN (SELECT COUNT(*) FROM menu m2 WHERE m2.parent_id_menu = m.id_menu) = 0
        THEN
            CASE
                WHEN m.feature_code IS NULL THEN '❌ ERROR: Hoja sin feature_code (se requiere)'
                ELSE '✅ OK: Hoja con feature_code (se valida)'
            END
        ELSE
            CASE
                WHEN m.feature_code IS NOT NULL THEN '✅ OK: Padre con feature_code (se ignora, solo referencia)'
                ELSE '✅ OK: Padre sin feature_code (también válido)'
            END
    END as estado
FROM menu m
WHERE m.status = true
ORDER BY hijos_count DESC, m.id_menu;
```

---

## ✅ Ventajas de Esta Arquitectura

1. **Granularidad:** Cada menú hoja tiene su propio permiso
2. **Flexibilidad:** Los padres se adaptan automáticamente según los hijos
3. **Simplicidad:** No necesitas mantener `feature_code` en padres
4. **Escalabilidad:** Fácil agregar nuevos menús sin modificar código
5. **Consistencia:** Un solo punto de verdad (menús hoja)

---

## 🚨 Casos Especiales

### Caso 1: Menú Padre con URL pero Sin Hijos

Si un menú padre tiene `url` pero también tiene hijos, la URL se ignora y solo se valida por los hijos.

### Caso 2: Menú con URL y Sin Feature Code

Si un menú hoja no tiene `feature_code`, se muestra automáticamente (ruta pública).

### Caso 3: Menú Padre con Feature Code

Si un menú padre tiene `feature_code`, se ignora en la validación (solo se usa para referencia/documentación). La validación se hace solo en los hijos.

---

## 📝 Resumen

**Arquitectura:**

- ✅ Menús hoja → Tienen `feature_code` → Se validan con `canAccess()`
- ✅ Menús padre → NO tienen `feature_code` → Se muestran si tienen hijos visibles
- ✅ Validación bottom-up → Primero hijos, luego padres

**Implementación:**

- ✅ Ya está implementado en `filterSubMenus()`
- ⚠️ Solo falta asegurar que los padres NO validen su `feature_code` (ya está así)

**Base de Datos:**

- ✅ Asignar `feature_code` a menús hoja (obligatorio, se valida)
- ✅ Los menús padre pueden tener `feature_code` (opcional, se ignora en validación)
