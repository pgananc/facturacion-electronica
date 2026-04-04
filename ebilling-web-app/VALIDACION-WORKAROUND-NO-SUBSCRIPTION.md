# 🔍 Validación Workaround NO_SUBSCRIPTION

## 📋 Contexto

**Workaround ubicación:** `permission.service.ts` líneas 408-430

**Problema original:**

- Backend retornaba `NO_SUBSCRIPTION` (403 con `reason='NO_SUBSCRIPTION'`) incorrectamente
- Esto ocurría incluso cuando el usuario SÍ tenía suscripción activa pero el plan no incluía la feature
- El frontend tenía suscripción cargada en caché pero el backend la ignoraba

**Solución temporal (workaround):**

- Si backend retorna `NO_SUBSCRIPTION` pero tenemos suscripción en caché
- Verificar localmente si la feature está habilitada para el plan usando `planPermissionsCache`
- Si está habilitada → Permitir acceso temporalmente
- Si NO está habilitada → Denegar acceso correctamente

---

## 🧪 Plan de Validación

### **Paso 1: Preparar Ambiente de Prueba**

1. **Usuario de prueba:** `admin` o `member` con plan `BASIC` asignado
2. **Suscripción activa:** Verificar que la compañía tiene suscripción `ACTIVE` con plan `BASIC`
3. **Feature de prueba:** Usar una feature que:
   - ✅ Está habilitada para plan `BASIC` (debe funcionar)
   - ❌ NO está habilitada para plan `BASIC` (debe denegar acceso)

### **Paso 2: Escenarios de Prueba**

#### **Escenario A: Feature HABILITADA para el plan**

**Setup:**

- Usuario: `admin` con plan `BASIC`
- Feature: `CLIENT_CREATE` (debe estar habilitada para BASIC)
- Suscripción: `ACTIVE` con plan `BASIC`

**Acción:**

1. Login como `admin`
2. Intentar acceder a `/clients` (o usar feature `CLIENT_CREATE`)
3. Abrir DevTools → Network → Ver respuesta del backend
4. Abrir DevTools → Console → Ver logs

**Resultado esperado si backend está arreglado:**

- ✅ Backend retorna `200 OK` con `{ allowed: true }`
- ✅ NO aparece log: `"⚠️ Backend retorna NO_SUBSCRIPTION"`
- ✅ Acceso permitido

**Resultado si backend todavía falla:**

- ❌ Backend retorna `403` con `reason='NO_SUBSCRIPTION'`
- ⚠️ Aparece log: `"⚠️ Backend retorna NO_SUBSCRIPTION pero la feature SÍ está habilitada"`
- ✅ Acceso permitido (gracias al workaround)

---

#### **Escenario B: Feature NO HABILITADA para el plan**

**Setup:**

- Usuario: `admin` con plan `BASIC`
- Feature: `REPORT_ADVANCED` (NO debe estar habilitada para BASIC)
- Suscripción: `ACTIVE` con plan `BASIC`

**Acción:**

1. Login como `admin`
2. Intentar acceder a `/reports/sales-dashboard` (o usar feature `REPORT_ADVANCED`)
3. Abrir DevTools → Network → Ver respuesta del backend
4. Abrir DevTools → Console → Ver logs

**Resultado esperado si backend está arreglado:**

- ✅ Backend retorna `403` con `reason='PLAN_DOES_NOT_INCLUDE_FEATURE'` o similar
- ✅ NO aparece log: `"⚠️ Backend retorna NO_SUBSCRIPTION"`
- ✅ Acceso denegado correctamente

**Resultado si backend todavía falla:**

- ❌ Backend retorna `403` con `reason='NO_SUBSCRIPTION'` (incorrecto)
- ⚠️ Aparece log: `"⚠️ Backend retorna NO_SUBSCRIPTION pero la feature NO está habilitada"`
- ✅ Acceso denegado correctamente (workaround funciona)

---

### **Paso 3: Verificar Logs en Consola**

Buscar estos logs específicos en la consola del navegador:

#### **Si backend está arreglado:**

```
✅ NO debe aparecer:
- "⚠️ Backend retorna NO_SUBSCRIPTION pero la feature SÍ está habilitada"
- "⚠️ Problema con suscripción para [featureCode]"
```

#### **Si backend todavía falla:**

```
⚠️ Debe aparecer:
- "⚠️ Backend retorna NO_SUBSCRIPTION pero la feature SÍ está habilitada para plan [planCode]"
- "⚠️ Problema con suscripción para [featureCode]"
```

---

### **Paso 4: Verificar Respuesta del Backend**

En DevTools → Network → Seleccionar request a `/api/permission/validate-feature`

**Respuesta esperada si backend está arreglado:**

```json
// Feature habilitada para el plan:
{
  "allowed": true,
  "reason": null,
  "message": "Acceso permitido"
}

// Feature NO habilitada para el plan:
{
  "allowed": false,
  "reason": "PLAN_DOES_NOT_INCLUDE_FEATURE",
  "message": "Su plan actual no incluye esta funcionalidad"
}
```

**Respuesta si backend todavía falla:**

```json
// Feature habilitada pero backend retorna NO_SUBSCRIPTION:
{
  "allowed": false,
  "reason": "NO_SUBSCRIPTION", // ❌ INCORRECTO
  "message": "Su plan actual no incluye esta funcionalidad"
}
```

---

## ✅ Criterios de Decisión

### **Si backend está arreglado:**

- ✅ Backend retorna `reason='PLAN_DOES_NOT_INCLUDE_FEATURE'` cuando el plan no incluye la feature
- ✅ Backend retorna `reason='NO_SUBSCRIPTION'` SOLO cuando realmente NO hay suscripción
- ✅ NO aparece el log de workaround en consola
- ✅ **ACCIÓN:** Remover workaround (líneas 408-430)

### **Si backend todavía falla:**

- ❌ Backend retorna `reason='NO_SUBSCRIPTION'` incluso cuando hay suscripción activa
- ⚠️ Aparece el log de workaround en consola
- ✅ **ACCIÓN:** Mantener workaround y coordinar fix con backend

---

## 📝 Checklist de Validación

- [ ] Preparar usuario de prueba con plan `BASIC`
- [ ] Verificar que tiene suscripción `ACTIVE`
- [ ] Probar Escenario A (feature habilitada)
- [ ] Probar Escenario B (feature NO habilitada)
- [ ] Revisar logs en consola
- [ ] Revisar respuesta del backend en Network
- [ ] Documentar resultados
- [ ] Decidir: ¿Remover o mantener workaround?

---

---

## 📊 Verificación de Feature Codes en DB

**Migraciones de Flyway relacionadas:**

- ✅ `V2025121701__add_feature_code_to_menu.sql` (migración 24)
- ✅ `V2025122704__granularize_menu_feature_codes.sql` (migración 34)

**Script de verificación:** `VERIFICAR-FEATURE-CODES-DB.sql`

**Antes de validar el workaround, ejecutar:**

```sql
-- Verificar que todos los menús hoja tienen feature_code
SELECT COUNT(*) as menus_sin_feature_code
FROM menu m
WHERE m.active = true
  AND m.url IS NOT NULL
  AND m.url != 'null'
  AND (SELECT COUNT(*) FROM menu m2 WHERE m2.parent_id_menu = m.id) = 0
  AND (m.feature_code IS NULL OR m.feature_code = '');
```

**Resultado esperado:** `0` (todos los menús hoja tienen feature_code)

---

**Fecha de validación:** ****\_\_\_****  
**Validado por:** ****\_\_\_****  
**Resultado:** [ ] Backend arreglado / [ ] Backend todavía falla  
**Decisión:** [ ] Remover workaround / [ ] Mantener workaround
