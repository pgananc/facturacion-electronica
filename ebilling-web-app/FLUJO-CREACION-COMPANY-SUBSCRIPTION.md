# 🔄 Flujo de Creación de Compañía y Asignación de Suscripción

**Fecha:** 21 de Diciembre, 2025  
**Contexto:** Clarificación del flujo esperado vs flujo actual

---

## 📋 **FLUJO ACTUAL (Cómo funciona ahora)**

### **1. Crear una Compañía**
```
Usuario → Frontend → POST /api/company
                    ↓
              Backend crea compañía
                    ↓
         Retorna: { idCompany, name, ... }
                    ↓
         ❌ NO se asigna suscripción automáticamente
```

**Resultado:**
- ✅ Compañía creada en BD
- ❌ **NO tiene suscripción** (no aparece en `company_subscription`)
- ⚠️ La compañía **NO puede emitir documentos** (no tiene plan activo)

---

### **2. Asignar Suscripción (Manual)**
```
Super Admin → Dialog "Asignar Suscripción"
                    ↓
         Selecciona compañía sin suscripción
                    ↓
         POST /api/subscriptions
                    ↓
         Backend crea registro en company_subscription
                    ↓
         ✅ Compañía ahora tiene suscripción ACTIVA
```

**Resultado:**
- ✅ Suscripción asignada
- ✅ Compañía puede emitir documentos (según límites del plan)

---

## 🎯 **FLUJO ESPERADO (Cómo DEBERÍA funcionar)**

### **Opción A: Asignación Automática de Plan FREE**

```
Usuario → Frontend → POST /api/company
                    ↓
              Backend crea compañía
                    ↓
         🔄 TRIGGER o LÓGICA AUTOMÁTICA:
         - Asigna plan FREE automáticamente
         - start_date = TODAY
         - expires_at = TODAY + 10 years
         - status = 'ACTIVE'
                    ↓
         Retorna: { idCompany, name, ... }
                    ↓
         ✅ Compañía creada CON suscripción FREE
```

**Ventajas:**
- ✅ Compañía puede usar el sistema inmediatamente
- ✅ No requiere acción manual del Super Admin
- ✅ Flujo más simple para el usuario

**Desventajas:**
- ⚠️ Requiere modificar el backend (trigger o lógica en el servicio)
- ⚠️ Todas las compañías nuevas empiezan con FREE (puede no ser deseado)

---

### **Opción B: Asignación Manual (Actual) con Mejoras**

```
Usuario → Frontend → POST /api/company
                    ↓
              Backend crea compañía
                    ↓
         Retorna: { idCompany, name, ... }
                    ↓
         ⚠️ Compañía SIN suscripción
                    ↓
         Super Admin → Ve notificación/lista de compañías sin suscripción
                    ↓
         Asigna suscripción manualmente
                    ↓
         ✅ Compañía ahora tiene suscripción
```

**Mejoras sugeridas:**
1. **Notificación al Super Admin** cuando se crea una compañía sin suscripción
2. **Lista destacada** de compañías sin suscripción en el dashboard
3. **Dialog automático** después de crear compañía: "¿Desea asignar una suscripción ahora?"

---

## 🔍 **ANÁLISIS DEL PROBLEMA ACTUAL**

### **¿Por qué tu nueva compañía no aparece en `/api/subscriptions/available-companies`?**

**Razón 1: Error 500 en el Backend**
- El endpoint está fallando con error 500
- No puede ejecutar la consulta SQL correctamente
- **Solución:** Arreglar el endpoint en el backend

**Razón 2: La compañía SÍ debería aparecer**
- Si la compañía NO tiene suscripción activa → ✅ Debe aparecer
- Si la compañía tiene suscripción activa → ❌ NO debe aparecer

**Razón 3: Fallback implementado**
- Si el endpoint falla, ahora cargamos TODAS las compañías
- Tu nueva compañía aparecerá en la lista (fallback)
- ⚠️ Pero incluirá también compañías CON suscripción (no ideal)

---

## ✅ **RECOMENDACIÓN: Flujo Híbrido**

### **Flujo Recomendado:**

1. **Crear Compañía:**
   ```
   POST /api/company → Crea compañía SIN suscripción
   ```

2. **Backend: Trigger Automático (Opcional pero Recomendado)**
   ```sql
   -- Trigger que asigna plan FREE automáticamente
   CREATE OR REPLACE FUNCTION assign_free_plan_on_company_create()
   RETURNS TRIGGER AS $$
   DECLARE
     free_plan_id INT;
   BEGIN
     SELECT id_subscription_plan INTO free_plan_id 
     FROM subscription_plan WHERE code = 'FREE';
     
     INSERT INTO company_subscription (
       id_company, id_subscription_plan, 
       start_date, expires_at, status, 
       payment_method, notes
     ) VALUES (
       NEW.id_company, free_plan_id,
       CURRENT_DATE, CURRENT_DATE + INTERVAL '10 years',
       'ACTIVE', 'MANUAL',
       'Plan FREE asignado automáticamente al crear la compañía'
     );
     
     RETURN NEW;
   END;
   $$ LANGUAGE plpgsql;

   CREATE TRIGGER trigger_assign_free_plan
   AFTER INSERT ON company
   FOR EACH ROW
   EXECUTE FUNCTION assign_free_plan_on_company_create();
   ```

3. **Super Admin puede actualizar después:**
   - Si quiere cambiar de FREE a otro plan → Usa "Asignar Suscripción"
   - Si quiere extender → Usa "Extender Suscripción"
   - Si quiere cambiar plan → Usa "Cambiar Plan"

---

## 📊 **COMPARACIÓN DE OPCIONES**

| Aspecto | Opción A (Auto FREE) | Opción B (Manual) |
|--------|---------------------|-------------------|
| **Simplicidad** | ✅ Más simple | ❌ Requiere acción manual |
| **Control** | ⚠️ Menos control | ✅ Control total |
| **UX** | ✅ Mejor UX | ⚠️ UX más compleja |
| **Implementación** | ⚠️ Requiere trigger | ✅ Ya implementado |
| **Flexibilidad** | ✅ Puede cambiar después | ✅ Puede asignar cualquier plan |

---

## 🎯 **DECISIÓN RECOMENDADA**

**Implementar Opción A (Auto FREE) con posibilidad de actualizar:**

1. ✅ **Backend:** Agregar trigger que asigna FREE automáticamente
2. ✅ **Frontend:** Mantener funcionalidad de "Asignar Suscripción" para cambiar/actualizar
3. ✅ **Resultado:** 
   - Compañías nuevas pueden usar el sistema inmediatamente
   - Super Admin puede actualizar a otro plan cuando lo desee
   - Mejor experiencia de usuario

---

## 🔧 **IMPLEMENTACIÓN SUGERIDA**

### **Backend (PostgreSQL Trigger):**
```sql
-- Ver código arriba en "Flujo Recomendado"
```

### **Frontend (Sin cambios necesarios):**
- El flujo actual ya funciona
- Solo necesita que el backend asigne FREE automáticamente

---

## ❓ **PREGUNTAS PARA DECIDIR**

1. **¿Queremos que todas las compañías nuevas empiecen con FREE automáticamente?**
   - ✅ SÍ → Implementar trigger
   - ❌ NO → Mantener flujo manual actual

2. **¿El Super Admin debe aprobar cada suscripción?**
   - ✅ SÍ → Mantener flujo manual
   - ❌ NO → Implementar auto-asignación FREE

3. **¿Hay casos donde NO queremos asignar FREE automáticamente?**
   - Si hay casos especiales → Mantener flujo manual
   - Si siempre queremos FREE → Implementar trigger

---

## 📝 **CONCLUSIÓN**

**Flujo Actual:**
- Crear compañía → NO tiene suscripción → Super Admin debe asignar manualmente

**Flujo Esperado (Recomendado):**
- Crear compañía → Se asigna FREE automáticamente → Super Admin puede actualizar después

**Problema Actual:**
- El endpoint `/api/subscriptions/available-companies` falla con error 500
- Necesita ser arreglado en el backend
- Mientras tanto, el fallback muestra todas las compañías

