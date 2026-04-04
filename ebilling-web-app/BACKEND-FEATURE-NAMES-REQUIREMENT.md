# 🔧 Requerimiento Backend: Nombres Amigables de Features

## 📋 Problema Actual

El frontend está usando un mapeo hardcodeado de nombres amigables porque el backend no está enviando correctamente los nombres desde la base de datos. Esto es una solución temporal que debe ser corregida en el backend.

## ✅ Solución Requerida

El backend debe enviar los nombres amigables de las features desde la base de datos, priorizando el nombre del menú sobre el nombre de la tabla `feature_permission`.

---

## 🗄️ Fuentes de Datos

### 1. Tabla `menu` (Fuente Principal)

La tabla `menu` contiene los nombres amigables que los usuarios ven en la interfaz:

```sql
SELECT
    id_menu,
    name,              -- Nombre amigable (ej: "Ventas por periodo", "Gestionar Punto de Emisión")
    url,
    feature_code
FROM menu
WHERE status = true
  AND feature_code IS NOT NULL;
```

**Ejemplo:**

- `name`: "Ventas por periodo"
- `feature_code`: "REPORT_SALES_BY_PERIOD"

### 2. Tabla `feature_permission` (Fuente Secundaria)

La tabla `feature_permission` puede tener `feature_name`, pero debe ser consistente con el menú:

```sql
SELECT
    feature_code,
    feature_name,      -- Debe coincidir con menu.name
    description,
    module
FROM feature_permission
WHERE is_active = true;
```

---

## 🔄 Endpoints que Deben Corregirse

### 1. `GET /api/permission/permissions/features`

**Problema Actual:**

- Puede estar retornando `feature_code` como `name`
- O puede estar retornando `feature_name` vacío o igual al `feature_code`

**Solución Requerida:**

```sql
-- Query sugerida para el backend
SELECT
    fp.feature_code,
    COALESCE(m.name, fp.feature_name, fp.feature_code) as name,  -- Priorizar menu.name
    fp.module,
    fp.allowed_roles,
    fp.required_plans,
    COALESCE(fp.description, '') as description
FROM feature_permission fp
LEFT JOIN menu m ON m.feature_code = fp.feature_code AND m.status = true
WHERE fp.is_active = true
ORDER BY fp.module, COALESCE(m.name, fp.feature_name, fp.feature_code);
```

**Lógica de Prioridad:**

1. `menu.name` (nombre amigable del menú)
2. `feature_permission.feature_name` (si existe)
3. `feature_code` (último recurso)

### 2. `GET /api/permission/permissions/features/plans`

**Mismo problema y solución que el endpoint anterior.**

### 3. `GET /api/menu/{userName}`

**Este endpoint ya está correcto** - retorna `name` amigable para cada menú hoja.

---

## 📝 Script SQL para Actualizar `feature_permission.feature_name`

Si algunos registros en `feature_permission` no tienen `feature_name` o tienen el `feature_code` como nombre, actualizar desde la tabla `menu`:

```sql
-- Actualizar feature_name desde menu.name
UPDATE feature_permission fp
SET feature_name = m.name
FROM menu m
WHERE m.feature_code = fp.feature_code
  AND m.status = true
  AND (fp.feature_name IS NULL
       OR fp.feature_name = ''
       OR fp.feature_name = fp.feature_code);

-- Verificar resultado
SELECT
    fp.feature_code,
    fp.feature_name,
    m.name as menu_name,
    CASE
        WHEN fp.feature_name = m.name THEN '✅ CORRECTO'
        WHEN fp.feature_name IS NULL OR fp.feature_name = '' THEN '⚠️ VACÍO'
        WHEN fp.feature_name = fp.feature_code THEN '❌ IGUAL A FEATURE_CODE'
        ELSE '⚠️ DIFERENTE'
    END as status
FROM feature_permission fp
LEFT JOIN menu m ON m.feature_code = fp.feature_code AND m.status = true
WHERE fp.is_active = true
ORDER BY status, fp.feature_code;
```

---

## ✅ Checklist de Validación Backend

- [ ] El endpoint `/api/permission/permissions/features` retorna `name` amigable (no `feature_code`)
- [ ] El endpoint `/api/permission/permissions/features/plans` retorna `name` amigable (no `feature_code`)
- [ ] La tabla `feature_permission.feature_name` está sincronizada con `menu.name`
- [ ] Si un `feature_code` no tiene menú asociado, `feature_name` tiene un valor amigable
- [ ] Los nombres son consistentes entre ambos endpoints

---

## 🔄 Cambios en Frontend (Temporal)

El frontend actualmente tiene un mapeo hardcodeado como **fallback temporal**. Una vez que el backend envíe los nombres correctamente, este mapeo puede ser removido.

**Ubicación del código temporal:**

- `src/app/_service/permission/permission-admin.service.ts`
- Método: `getFeatureFriendlyName()`

**Nota:** Este código es un **workaround temporal** hasta que el backend envíe los nombres correctamente.

---

## 📧 Notas para el Backend

1. **Prioridad de Nombres:**

   - Siempre usar `menu.name` si existe
   - Si no existe menú, usar `feature_permission.feature_name`
   - Solo usar `feature_code` como último recurso

2. **Sincronización:**

   - Cuando se crea/actualiza un menú con `feature_code`, actualizar `feature_permission.feature_name`
   - Mantener consistencia entre ambas tablas

3. **Validación:**
   - No retornar `feature_code` como `name` si hay un nombre amigable disponible
   - Validar que `feature_name` no esté vacío o igual al `feature_code`

---

**Fecha de Creación:** 2025-01-XX  
**Prioridad:** Media  
**Impacto:** Mejora la experiencia del usuario y elimina código hardcodeado del frontend
