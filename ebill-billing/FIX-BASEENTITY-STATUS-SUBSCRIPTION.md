# 🔧 Fix: BaseEntity Status Column Issue

## 🐛 Problema Encontrado

Al iniciar la aplicación con las nuevas tablas de suscripción, se produjo un error:

```
ERROR: column "status" of relation "subscription_plan" contains null values
```

## 📋 Causa Raíz

Las entidades `SubscriptionPlan`, `CompanySubscription` y `SubscriptionUsageHistory` extienden de `BaseEntity`, que incluye un campo `status` (Boolean) heredado. Sin embargo:

1. La migración inicial (`V2025121401`) creó las tablas sin la columna `status`
2. Los INSERT de planes no incluyeron el campo `status`
3. Hibernate intentó agregar la columna `status` en tiempo de ejecución
4. Como las filas ya existían sin valor para `status`, falló con constraint NOT NULL

## ✅ Solución Implementada

### 1. Nueva Migración: `V2025121402__add_status_to_subscription_tables.sql`

```sql
-- Add status column with default value
ALTER TABLE subscription_plan
ADD COLUMN IF NOT EXISTS status BOOLEAN NOT NULL DEFAULT TRUE;

-- Update existing rows
UPDATE subscription_plan
SET status = TRUE
WHERE status IS NULL;

-- Same for subscription_usage_history
ALTER TABLE subscription_usage_history
ADD COLUMN IF NOT EXISTS status BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE subscription_usage_history
SET status = TRUE
WHERE status IS NULL;
```

### 2. Ajuste en `CompanySubscription.java`

**Problema:** Había conflicto de nombres entre:

- `BaseEntity.status` (Boolean) - inherited
- Campo personalizado `status` (String) para estado de suscripción

**Solución:** Renombrar el campo personalizado a `subscriptionStatus`:

```java
@Column(name = "subscription_status", nullable = false, length = 20)
String subscriptionStatus; // ACTIVE, EXPIRED, CANCELLED, SUSPENDED
```

### 3. Agregar `@EqualsAndHashCode(callSuper = false)`

Agregado a todas las entidades que extienden BaseEntity:

- `SubscriptionPlan`
- `CompanySubscription`
- `SubscriptionUsageHistory`

## 📊 Estructura Final de Tablas

### `subscription_plan`

```sql
CREATE TABLE subscription_plan (
    id_plan BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    -- ... otros campos ...
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP,
    status BOOLEAN NOT NULL DEFAULT TRUE  -- ✅ Heredado de BaseEntity
);
```

### `company_subscription`

```sql
CREATE TABLE company_subscription (
    id_subscription BIGSERIAL PRIMARY KEY,
    id_company BIGINT NOT NULL,
    id_plan BIGINT NOT NULL,
    subscription_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ✅ Estado de suscripción
    -- ... otros campos ...
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP,
    status BOOLEAN NOT NULL DEFAULT TRUE  -- ✅ Heredado de BaseEntity
);
```

### `subscription_usage_history`

```sql
CREATE TABLE subscription_usage_history (
    id_history BIGSERIAL PRIMARY KEY,
    -- ... otros campos ...
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date TIMESTAMP,
    status BOOLEAN NOT NULL DEFAULT TRUE  -- ✅ Heredado de BaseEntity
);
```

## 🔄 Orden de Migraciones

1. **V2025121401** - Crear tablas, insertar planes, migrar companies
2. **V2025121402** - Agregar columna `status` a las tablas existentes ✅

## ✅ Verificación

Después de aplicar las migraciones, verificar:

```sql
-- Verificar que todas las columnas existan
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'subscription_plan'
  AND column_name IN ('status', 'is_active');

-- Verificar que los planes tengan status = TRUE
SELECT code, name, is_active, status
FROM subscription_plan;

-- Verificar subscripciones
SELECT id_company, subscription_status, status
FROM company_subscription;
```

**Resultado Esperado:**

- Todas las tablas tienen columna `status` (Boolean)
- Todos los registros tienen `status = TRUE`
- `company_subscription` usa `subscription_status` (String) para el estado de la suscripción

## 🚀 Reiniciar Aplicación

```bash
# Detener aplicación actual
# Reiniciar
mvn spring-boot:run
```

**Resultado Esperado:**

```
✅ Migrating schema "public" to version "2025121402 - add status to subscription tables"
✅ Successfully applied 1 migration
✅ Application started successfully on port 8480
```

## 📝 Lecciones Aprendidas

1. **Siempre considerar campos heredados** de clases base al diseñar migraciones
2. **Evitar conflictos de nombres** entre campos heredados y personalizados
3. **Usar nombres descriptivos** como `subscription_status` vs `status`
4. **Incluir DEFAULT values** en ALTER TABLE ADD COLUMN para tablas con datos
5. **Agregar `@EqualsAndHashCode(callSuper = false)`** en entidades que extienden BaseEntity

## ✅ Estado Actual

- ✅ Todos los campos de BaseEntity considerados
- ✅ Conflicto de nombres resuelto
- ✅ Migraciones corregidas
- ✅ Entidades actualizadas
- ✅ Repositorios actualizados
- ✅ Servicios actualizados
- ✅ Sin errores de linter
- ✅ Aplicación lista para iniciar

---

**Fecha de Fix:** 14 de Diciembre, 2025  
**Versión:** 1.0.1
