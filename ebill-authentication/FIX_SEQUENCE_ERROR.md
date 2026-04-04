# 🔧 FIX: Error de Secuencia en V2025121702

## ❌ Problema Encontrado

Al ejecutar la migración `V2025121702__assign_admin_role_menus.sql`, falló con el siguiente error:

```
ERROR: duplicate key value violates unique constraint "menu_role_pkey"
Detail: Key (id_menu_role)=(1) already exists.
```

## 🔍 Causa Raíz

La secuencia `menu_role_seq` estaba desincronizada con los datos existentes en la tabla `menu_role`. La secuencia estaba en un valor bajo (ej: 1), pero ya existían registros con IDs 1, 2, 3, etc.

Cuando la migración intentó insertar usando `nextval('menu_role_seq')`, generó IDs que ya existían, causando el error de clave duplicada.

## ✅ Solución Implementada

Se agregó un bloque `DO` al inicio de la migración que sincroniza automáticamente la secuencia:

```sql
-- FIX: Sync menu_role_seq with existing data
DO $$
DECLARE
    max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id_menu_role), 0) INTO max_id FROM public.menu_role;
    PERFORM setval('menu_role_seq', max_id);
END $$;
```

Este código:
1. Obtiene el `MAX(id_menu_role)` actual de la tabla
2. Setea la secuencia a ese valor máximo
3. El siguiente `nextval()` generará `max_id + 1`, evitando duplicados

## 🚀 Pasos para Aplicar el Fix

### 1. Verificar que el archivo está actualizado

El archivo ya está corregido en:
```
src/main/resources/flyway/V2025121702__assign_admin_role_menus.sql
```

### 2. Reiniciar la aplicación

**Opción A: Desde el IDE** (Recomendado)
- Detén la aplicación actual (si está corriendo)
- Haz click en "Run" nuevamente
- El IDE compilará automáticamente y copiará los archivos actualizados

**Opción B: Desde terminal**
```bash
# Limpiar y compilar
mvn clean compile

# Iniciar aplicación
mvn spring-boot:run -Dspring-boot.run.profiles=localhost
```

### 3. Verificar que la migración se ejecutó correctamente

Deberías ver en el log:
```
INFO  o.f.core.internal.command.DbMigrate  : Current version: 2025121701
INFO  o.f.core.internal.command.DbMigrate  : Migrating to: 2025121702 - assign admin role menus
INFO  o.f.core.internal.command.DbMigrate  : Migrating to: 2025121703 - assign admin role features
INFO  o.f.core.internal.command.DbMigrate  : Successfully applied 2 migrations
```

## 🧪 Validación Post-Fix

Después de que la aplicación inicie correctamente, ejecuta:

```sql
-- 1. Verificar que se crearon los 28 menús para Admin
SELECT COUNT(*) FROM menu_role WHERE id_role = 3;
-- Esperado: 28

-- 2. Verificar que la secuencia está sincronizada
SELECT currval('menu_role_seq'), MAX(id_menu_role) FROM menu_role;
-- currval debe ser >= MAX(id_menu_role)

-- 3. Probar que se pueden insertar nuevos registros
INSERT INTO menu_role (id_menu_role, id_menu, id_role, status)
VALUES (nextval('menu_role_seq'), 99, 1, false);
-- Debe ejecutarse sin error

-- Limpiar el test
DELETE FROM menu_role WHERE id_menu = 99;
```

## 📋 Checklist de Resolución

- [x] Archivo fuente corregido (`V2025121702__assign_admin_role_menus.sql`)
- [x] Bloque de sincronización agregado
- [x] Documentación actualizada (`ADMIN_ROLE_PERMISSIONS.md`)
- [ ] **Aplicación reiniciada** (pendiente - hazlo ahora)
- [ ] **Migración ejecutada exitosamente** (pendiente - verificar log)
- [ ] **Validación SQL ejecutada** (pendiente - opcional)

## 🎯 Próximo Paso

**REINICIA LA APLICACIÓN AHORA** desde tu IDE (botón Run/Debug).

La migración debería ejecutarse exitosamente esta vez.

---

**Fecha del Fix**: 2025-12-18
**Archivo Afectado**: `V2025121702__assign_admin_role_menus.sql`
**Estado**: ✅ CORREGIDO
