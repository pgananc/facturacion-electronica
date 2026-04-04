# 🛠️ Guía: Funciones Helper para Migraciones Flyway

## 📋 Propósito

Evitar problemas de IDs hardcodeados en migraciones Flyway cuando múltiples desarrolladores trabajan en paralelo.

---

## ✅ Funciones Disponibles

### 1. `get_next_menu_id()`
Obtiene el siguiente ID disponible para la tabla `menu`.

**Uso:**
```sql
INSERT INTO public.menu (id_menu, parent_id_menu, name, url, icon, status, feature_code)
VALUES (
    get_next_menu_id(),  -- ✅ Dinámico, seguro
    16,
    'Nuevo Menú',
    '/new-menu',
    'icon_name',
    true,
    'NEW_FEATURE_CODE'
)
ON CONFLICT (id_menu) DO NOTHING;
```

---

### 2. `get_next_menu_role_id()`
Obtiene el siguiente ID disponible para la tabla `menu_role`.

**Uso:**
```sql
INSERT INTO public.menu_role (id_menu_role, id_menu, id_role, status)
VALUES (
    get_next_menu_role_id(),  -- ✅ Dinámico, seguro
    32,
    1,
    true
)
ON CONFLICT (id_menu_role) DO NOTHING;
```

---

### 3. `get_menu_id_by_name(p_name, p_parent_id)`
Busca un menú existente por nombre y parent_id opcional.

**Uso:**
```sql
DO $$
DECLARE
    menu_id INTEGER;
BEGIN
    menu_id := get_menu_id_by_name('Gestión de Permisos', 16);
    
    IF menu_id IS NULL THEN
        -- Crear nuevo menú
        menu_id := get_next_menu_id();
        INSERT INTO public.menu (id_menu, parent_id_menu, name, ...)
        VALUES (menu_id, 16, 'Gestión de Permisos', ...);
    END IF;
    
    -- Usar menu_id para otras operaciones
END $$;
```

---

### 4. `get_menu_id_by_feature_code(p_feature_code)`
Busca un menú existente por feature_code.

**Uso:**
```sql
DO $$
DECLARE
    menu_id INTEGER;
BEGIN
    menu_id := get_menu_id_by_feature_code('ROLE_PERMISSION_MANAGEMENT');
    
    IF menu_id IS NOT NULL THEN
        -- Actualizar menú existente
        UPDATE public.menu SET url = '/new-url' WHERE id_menu = menu_id;
    END IF;
END $$;
```

---

### 5. `insert_menu_safe(...)` ⭐ RECOMENDADA
Inserta un menú de forma segura o actualiza si ya existe. **Idempotente**.

**Uso:**
```sql
-- Insertar o actualizar menú en una sola llamada
SELECT insert_menu_safe(
    16,                              -- parent_id_menu
    'Gestión de Permisos',           -- name
    '/admin/role-permissions',       -- url
    'security',                      -- icon
    'ROLE_PERMISSION_MANAGEMENT'     -- feature_code (opcional)
) as menu_id;
```

**Ventajas:**
- ✅ Idempotente (puede ejecutarse múltiples veces)
- ✅ Actualiza si existe, crea si no existe
- ✅ Retorna el ID del menú (existente o nuevo)

---

### 6. `assign_menu_to_role_safe(p_menu_id, p_role_id)` ⭐ RECOMENDADA
Asigna un menú a un rol de forma segura. **Idempotente**.

**Uso:**
```sql
DO $$
DECLARE
    menu_id INTEGER;
    assignment_id BIGINT;
BEGIN
    -- Obtener o crear menú
    menu_id := insert_menu_safe(16, 'Nuevo Menú', '/new-menu', 'icon', 'FEATURE_CODE');
    
    -- Asignar a super_admin (rol 1)
    assignment_id := assign_menu_to_role_safe(menu_id, 1);
    
    RAISE NOTICE 'Menu ID: %, Assignment ID: %', menu_id, assignment_id;
END $$;
```

---

## 📝 Ejemplo Completo: Nueva Migración Flyway

```sql
-- ============================================
-- Script: Add New Feature Menu
-- Date: 2025-12-20
-- Purpose: Add new menu using helper functions
-- ============================================

DO $$
DECLARE
    menu_id INTEGER;
    assignment_id BIGINT;
BEGIN
    -- 1. Insertar o actualizar menú (idempotente)
    menu_id := insert_menu_safe(
        16,                              -- Parent: Administración Global
        'Nuevo Feature',                 -- Nombre
        '/admin/new-feature',            -- URL
        'new_icon',                      -- Icono
        'NEW_FEATURE_CODE'               -- Feature code
    );
    
    -- 2. Asignar a super_admin (rol 1)
    assignment_id := assign_menu_to_role_safe(menu_id, 1);
    
    RAISE NOTICE '✅ Menu creado/actualizado: ID %, Assignment ID: %', menu_id, assignment_id;
END $$;

-- ============================================
-- VERIFICATION
-- ============================================

-- Verificar que el menú fue creado
-- SELECT id_menu, name, url, feature_code
-- FROM public.menu
-- WHERE feature_code = 'NEW_FEATURE_CODE';

-- Verificar asignación
-- SELECT mr.id_menu_role, m.name, mr.id_role
-- FROM public.menu_role mr
-- INNER JOIN public.menu m ON m.id_menu = mr.id_menu
-- WHERE m.feature_code = 'NEW_FEATURE_CODE';
```

---

## 🎯 Patrón Recomendado para Futuras Migraciones

### ✅ BUENO (Usando funciones helper):

```sql
-- V2025121712__add_new_feature.sql
DO $$
DECLARE
    menu_id INTEGER;
BEGIN
    menu_id := insert_menu_safe(16, 'Nuevo Menú', '/new', 'icon', 'FEATURE');
    PERFORM assign_menu_to_role_safe(menu_id, 1);
END $$;
```

### ❌ MALO (IDs hardcodeados):

```sql
-- V2025121712__add_new_feature.sql
INSERT INTO menu (id_menu, ...) VALUES (41, ...);  -- ❌ Puede fallar
```

---

## 🔄 Migración de Migraciones Existentes

Las migraciones existentes (V2025121701 - V2025121710) ya tienen `ON CONFLICT DO NOTHING`, así que están relativamente seguras.

**Para futuras migraciones:** Usa siempre las funciones helper.

---

## ✅ Ventajas de Usar Funciones Helper

1. **Idempotencia**: Puedes ejecutar la migración múltiples veces sin errores
2. **Seguridad**: No hay conflictos de IDs entre desarrolladores
3. **Mantenibilidad**: Código más limpio y reutilizable
4. **Automatización**: Las secuencias se sincronizan automáticamente
5. **Colaboración**: Múltiples desarrolladores pueden trabajar sin conflictos

---

## 🧪 Testing

Puedes probar las funciones directamente en PostgreSQL:

```sql
-- Probar función
SELECT get_next_menu_id();

-- Probar inserción segura
SELECT insert_menu_safe(16, 'Test Menu', '/test', 'test_icon', 'TEST_FEATURE');

-- Verificar resultado
SELECT * FROM menu WHERE name = 'Test Menu';
```

---

## 📚 Referencia Rápida

| Función | Retorna | Uso Principal |
|---------|---------|---------------|
| `get_next_menu_id()` | INTEGER | Obtener siguiente ID de menu |
| `get_next_menu_role_id()` | BIGINT | Obtener siguiente ID de menu_role |
| `get_menu_id_by_name()` | INTEGER | Buscar menú por nombre |
| `get_menu_id_by_feature_code()` | INTEGER | Buscar menú por feature_code |
| `insert_menu_safe()` | INTEGER | Insertar/actualizar menú (idempotente) |
| `assign_menu_to_role_safe()` | BIGINT | Asignar menú a rol (idempotente) |

---

**Fecha de creación**: 2025-12-19  
**Migración**: `V2025121711__create_helper_functions.sql`  
**Status**: ✅ Listo para usar en futuras migraciones
