# 🚀 Quick Start - Sistema de Suscripciones

## ⚡ Inicio Rápido

### 1. Reiniciar la Aplicación

```bash
# Detener aplicación actual (Ctrl+C en terminal)
# Reiniciar:
mvn spring-boot:run
```

### 2. Verificar Logs de Inicio

Buscar en los logs:

```
✅ Migrating schema "public" to version "2025121401"
✅ Migrating schema "public" to version "2025121402"
✅ Successfully applied 2 migrations
✅ Tomcat started on port 8480
```

### 3. Verificar Base de Datos

```sql
-- Ver planes creados
SELECT code, name, document_limit_yearly, status
FROM subscription_plan;
-- Debe mostrar 5 planes (FREE, BASIC, PRO, BUSINESS, ENTERPRISE)

-- Ver companies migradas
SELECT COUNT(*)
FROM company_subscription
WHERE subscription_status = 'ACTIVE';
-- Debe mostrar todas las companies con plan FREE
```

---

## 🔧 ¿Qué se Corrigió?

### Problema

```
ERROR: column "status" of relation "subscription_plan" contains null values
```

### Solución

1. ✅ Nueva migración `V2025121402` agrega columna `status` a tablas
2. ✅ Campo renombrado: `status` → `subscriptionStatus` en `CompanySubscription`
3. ✅ Agregado `@EqualsAndHashCode(callSuper = false)` a todas las entidades

---

## 📋 Estructura del Sistema

### Tablas Creadas

- `subscription_plan` - 5 planes predefinidos
- `company_subscription` - Suscripciones activas por compañía
- `subscription_usage_history` - Historial de uso mensual

### Endpoints Disponibles

```
GET  /api/subscription/{idCompany}/current
GET  /api/subscription/{idCompany}/usage
POST /api/subscription/{idCompany}/can-emit
POST /api/subscription/{idCompany}/upgrade
```

### Validaciones Activas

- ✅ Validación al crear facturas
- ✅ Validación al crear notas de crédito
- ✅ Límites de documentos por plan
- ✅ Verificación de expiración

---

## 🧪 Prueba Rápida

### Test 1: Ver suscripción actual

```bash
curl http://localhost:8480/api/subscription/1/current
```

### Test 2: Verificar límites

```bash
curl -X POST http://localhost:8480/api/subscription/1/can-emit \
  -H "Content-Type: application/json" \
  -d '{"documentType":"INVOICE"}'
```

### Test 3: Ver uso

```bash
curl http://localhost:8480/api/subscription/1/usage
```

---

## 📚 Documentación Completa

- `ESTADO-FINAL-SUSCRIPCIONES.md` - Estado completo del sistema
- `FIX-BASEENTITY-STATUS-SUBSCRIPTION.md` - Detalles técnicos del fix
- `API-REFERENCE-SUBSCRIPTION.md` - Documentación de API
- `QUICK-START-TESTING.md` - Guía de pruebas
- `IMPLEMENTACION-COMPLETA-SUSCRIPCIONES.md` - Overview técnico

---

## ✅ Checklist de Verificación

- [ ] Aplicación inicia sin errores
- [ ] Migraciones aplicadas (ver logs de Flyway)
- [ ] 5 planes en `subscription_plan`
- [ ] Companies migradas a plan FREE
- [ ] Endpoints responden correctamente
- [ ] Validación funciona en facturas

---

## 🆘 Si Algo Falla

### Error: "status contains null values"

➡️ **Solución:** Asegúrate que la migración `V2025121402` se ejecutó.

### Error: "subscriptionStatus not found"

➡️ **Solución:** Recompila el proyecto: `mvn clean compile`

### Error en Hibernate

➡️ **Solución:** Verifica que las entidades tengan `@EqualsAndHashCode(callSuper = false)`

---

**¡Listo para producción!** 🎉
