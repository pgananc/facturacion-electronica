# Propuesta: Parámetro Global para Habilitar/Deshabilitar Rate Limiting

## 🎯 Objetivo

Crear un parámetro en la base de datos que permita **habilitar o deshabilitar el rate limiting globalmente** sin necesidad de redeployar la aplicación.

---

## 📋 Solución Propuesta

### 1. **Parámetro en Base de Datos**

**Tabla:** `public.params`  
**Código del parámetro:** `RATE_LIMITING_ENABLED`  
**Valor:** `"true"` o `"false"` (como string)

**Descripción:** Controla si el rate limiting está activo globalmente en toda la aplicación.

---

## 🔄 Flujo de Funcionamiento

### **Cuando `RATE_LIMITING_ENABLED = "true"` (Habilitado):**
```
Cliente hace solicitud
  ↓
GenericRateLimitService.canMakeRequest()
  ↓
Verifica: ¿RATE_LIMITING_ENABLED = "true"?
  ↓ SÍ
Aplica rate limiting normalmente
  ↓
Valida contadores en Redis
  ↓
Permite o rechaza según límites
```

### **Cuando `RATE_LIMITING_ENABLED = "false"` (Deshabilitado):**
```
Cliente hace solicitud
  ↓
GenericRateLimitService.canMakeRequest()
  ↓
Verifica: ¿RATE_LIMITING_ENABLED = "true"?
  ↓ NO (está en "false")
Retorna true inmediatamente (permite todas las solicitudes)
  ↓
NO incrementa contadores
  ↓
NO valida límites
```

---

## 🏗️ Arquitectura de la Solución

### **Componente 1: Parámetro en Base de Datos**

**Migración Flyway:** `V2026010300__add_rate_limiting_global_toggle.sql`

```sql
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (
    (SELECT COALESCE(MAX(id_param), 0) + 1 FROM public.params), 
    'RATE_LIMITING_ENABLED', 
    NOW(), 
    'Habilita o deshabilita el rate limiting globalmente en toda la aplicación. Valores: true (habilitado) o false (deshabilitado)', 
    true, 
    'true'  -- Por defecto habilitado
);
```

**Valores posibles:**
- `"true"` - Rate limiting activo (comportamiento normal)
- `"false"` - Rate limiting desactivado (permite todas las solicitudes)

---

### **Componente 2: Método en GenericRateLimitService**

**Ubicación:** `src/main/java/com/wallparisoft/authentication/service/GenericRateLimitService.java`

**Nuevo método privado:**

```java
/**
 * Verifica si el rate limiting está habilitado globalmente.
 * 
 * @return true si está habilitado, false si está deshabilitado
 */
private boolean isRateLimitingEnabled() {
    try {
        Params param = paramsRepository.findParamsByCodeParam("RATE_LIMITING_ENABLED");
        if (param != null) {
            return Boolean.parseBoolean(param.getValueParam());
        }
    } catch (Exception e) {
        log.warn("Error al verificar si rate limiting está habilitado, asumiendo habilitado por defecto", e);
    }
    // Por defecto: habilitado (comportamiento seguro)
    return true;
}
```

---

### **Componente 3: Modificaciones en Métodos Existentes**

#### **Método: `canMakeRequest()`**

**Modificación:**

```java
@Override
public boolean canMakeRequest(String endpoint, RateLimitDimension dimension, String identifier) {
    // ⭐ NUEVO: Verificar si rate limiting está habilitado globalmente
    if (!isRateLimitingEnabled()) {
        log.debug("Rate limiting deshabilitado globalmente, permitiendo solicitud");
        return true; // Permite todas las solicitudes
    }
    
    // Código existente...
    String key = buildRateLimitKey(endpoint, dimension, identifier);
    String attempts = utilsRedis.getKey(key);
    // ... resto del código
}
```

#### **Método: `incrementRequest()`**

**Modificación:**

```java
@Override
public void incrementRequest(String endpoint, RateLimitDimension dimension, String identifier) {
    // ⭐ NUEVO: No incrementar si rate limiting está deshabilitado
    if (!isRateLimitingEnabled()) {
        log.debug("Rate limiting deshabilitado globalmente, no incrementando contador");
        return; // No hace nada
    }
    
    // Código existente...
    String key = buildRateLimitKey(endpoint, dimension, identifier);
    Long attempts = utilsRedis.incrementKey(key);
    // ... resto del código
}
```

---

## 📊 Impacto en los Métodos

### **Métodos que se Modifican:**

| Método | Modificación | Comportamiento cuando deshabilitado |
|--------|--------------|--------------------------------------|
| `canMakeRequest()` | Verificar al inicio | Retorna `true` (permite todo) |
| `incrementRequest()` | Verificar al inicio | No hace nada (no incrementa) |
| `getCurrentAttempts()` | Sin cambios | Puede seguir consultando (no afecta) |
| `getTimeUntilNextAttempt()` | Sin cambios | Puede seguir consultando (no afecta) |
| `clearAttempts()` | Sin cambios | Puede seguir limpiando (útil para testing) |

**Nota:** Los métodos de consulta (`getCurrentAttempts`, `getTimeUntilNextAttempt`) no se modifican porque pueden ser útiles incluso cuando el rate limiting está deshabilitado (para monitoreo, por ejemplo).

---

## 🔄 Flujo Completo con el Toggle

### **Escenario 1: Rate Limiting Habilitado (`RATE_LIMITING_ENABLED = "true"`)**

```
POST /api/mail/send
  ↓
UserServiceImpl.sendMailUser()
  ↓
rateLimitService.canMakeRequest("MAIL_SEND", EMAIL, mail)
  ↓
GenericRateLimitService.canMakeRequest()
  ↓
isRateLimitingEnabled() → true
  ↓
Valida contador en Redis
  ↓
Compara con límite máximo
  ↓
Permite o rechaza según límite
```

### **Escenario 2: Rate Limiting Deshabilitado (`RATE_LIMITING_ENABLED = "false"`)**

```
POST /api/mail/send
  ↓
UserServiceImpl.sendMailUser()
  ↓
rateLimitService.canMakeRequest("MAIL_SEND", EMAIL, mail)
  ↓
GenericRateLimitService.canMakeRequest()
  ↓
isRateLimitingEnabled() → false
  ↓
Retorna true inmediatamente (bypass)
  ↓
NO valida contadores
  ↓
NO incrementa contadores
  ↓
Permite la solicitud siempre
```

---

## 🎯 Ventajas de esta Solución

### ✅ **1. Control Dinámico**
- Se puede activar/desactivar sin redeploy
- Cambio inmediato al modificar el parámetro en BD
- Útil para emergencias o mantenimiento

### ✅ **2. Seguridad por Defecto**
- Si el parámetro no existe o hay error, asume `true` (habilitado)
- Previene desactivación accidental
- Comportamiento conservador

### ✅ **3. Granularidad**
- Controla TODOS los endpoints de rate limiting
- Una sola configuración para toda la aplicación
- Fácil de entender y mantener

### ✅ **4. Compatibilidad**
- No rompe código existente
- Los métodos siguen funcionando igual cuando está habilitado
- Fácil de revertir si es necesario

---

## ⚠️ Consideraciones Importantes

### **1. Cache del Parámetro**

**Problema:** Si se lee el parámetro en cada solicitud, puede ser costoso.

**Solución opcional:** Implementar cache con TTL corto (ej: 1-5 minutos):

```java
@Cacheable(value = "rateLimitingEnabled", key = "'RATE_LIMITING_ENABLED'")
private boolean isRateLimitingEnabled() {
    // ... código
}
```

**Nota:** Para esta implementación inicial, podemos leer directamente de BD. Si hay problemas de performance, se puede agregar cache después.

---

### **2. Orden de Verificación**

**Importante:** Verificar el toggle **ANTES** de cualquier otra validación:

```java
// ✅ CORRECTO
if (!isRateLimitingEnabled()) {
    return true; // Salir temprano
}
// Validaciones de rate limiting...

// ❌ INCORRECTO
// Validaciones de rate limiting...
if (!isRateLimitingEnabled()) {
    return true; // Ya se hizo trabajo innecesario
}
```

---

### **3. Logging**

**Recomendación:** Agregar logs cuando el rate limiting está deshabilitado:

```java
if (!isRateLimitingEnabled()) {
    log.warn("⚠️ Rate limiting está DESHABILITADO globalmente - todas las solicitudes serán permitidas");
    return true;
}
```

Esto ayuda a identificar cuando el rate limiting está desactivado (útil para debugging y seguridad).

---

### **4. Impacto en Contadores Existentes**

**Comportamiento:**
- Cuando se deshabilita: No se incrementan contadores nuevos
- Contadores existentes en Redis: Se mantienen (no se eliminan)
- Cuando se vuelve a habilitar: Los contadores existentes siguen siendo válidos

**Ventaja:** Permite deshabilitar temporalmente sin perder el estado de los contadores.

---

## 📝 Casos de Uso

### **Caso 1: Mantenimiento Programado**
```sql
-- Deshabilitar rate limiting durante mantenimiento
UPDATE public.params 
SET value_param = 'false' 
WHERE code_param = 'RATE_LIMITING_ENABLED';

-- Después del mantenimiento, volver a habilitar
UPDATE public.params 
SET value_param = 'true' 
WHERE code_param = 'RATE_LIMITING_ENABLED';
```

### **Caso 2: Emergencia (Ataque DDoS)**
```sql
-- Si hay un ataque, deshabilitar temporalmente para permitir tráfico legítimo
UPDATE public.params 
SET value_param = 'false' 
WHERE code_param = 'RATE_LIMITING_ENABLED';
```

### **Caso 3: Testing**
```sql
-- Durante pruebas de carga, deshabilitar rate limiting
UPDATE public.params 
SET value_param = 'false' 
WHERE code_param = 'RATE_LIMITING_ENABLED';
```

---

## 🔍 Verificación del Estado

### **Consulta SQL para verificar:**

```sql
-- Ver el estado actual
SELECT code_param, value_param, description_param 
FROM public.params 
WHERE code_param = 'RATE_LIMITING_ENABLED';
```

### **Desde el código (para debugging):**

Se puede agregar un endpoint administrativo (opcional) para verificar el estado:

```java
@GetMapping("/admin/rate-limiting/status")
public ResponseEntity<Map<String, Object>> getRateLimitingStatus() {
    boolean enabled = genericRateLimitService.isRateLimitingEnabled();
    return ResponseEntity.ok(Map.of(
        "enabled", enabled,
        "message", enabled ? "Rate limiting está habilitado" : "Rate limiting está deshabilitado"
    ));
}
```

**Nota:** Este endpoint es opcional y solo para debugging/monitoreo.

---

## 🛠️ Implementación Propuesta

### **Archivos a Modificar:**

1. **Nuevo archivo:** `src/main/resources/flyway/V2026010300__add_rate_limiting_global_toggle.sql`
   - Crear el parámetro en la base de datos

2. **Modificar:** `src/main/java/com/wallparisoft/authentication/service/GenericRateLimitService.java`
   - Agregar método `isRateLimitingEnabled()`
   - Modificar `canMakeRequest()` para verificar el toggle
   - Modificar `incrementRequest()` para verificar el toggle

3. **Opcional:** Agregar método público `isRateLimitingEnabled()` en la interfaz
   - Para que otros servicios puedan consultar el estado

---

## 📊 Resumen de Cambios

| Componente | Acción | Ubicación |
|------------|-------|-----------|
| **Migración Flyway** | Crear parámetro `RATE_LIMITING_ENABLED` | `V2026010300__add_rate_limiting_global_toggle.sql` |
| **GenericRateLimitService** | Agregar método `isRateLimitingEnabled()` | Método privado |
| **GenericRateLimitService** | Modificar `canMakeRequest()` | Verificar toggle al inicio |
| **GenericRateLimitService** | Modificar `incrementRequest()` | Verificar toggle al inicio |
| **IGenericRateLimitService** | Opcional: Agregar método público | Para consultar estado |

---

## ✅ Criterios de Éxito

La solución se considera exitosa cuando:

1. ✅ El parámetro `RATE_LIMITING_ENABLED` existe en la base de datos
2. ✅ Cuando está en `"false"`, todas las solicitudes son permitidas
3. ✅ Cuando está en `"true"`, el rate limiting funciona normalmente
4. ✅ No hay degradación de performance significativa
5. ✅ Los logs indican cuando el rate limiting está deshabilitado
6. ✅ El cambio es inmediato (sin necesidad de reiniciar)

---

## 🔄 Plan de Implementación

### **Fase 1: Crear Parámetro**
- Crear migración Flyway
- Insertar parámetro con valor `"true"` por defecto

### **Fase 2: Implementar Lógica**
- Agregar método `isRateLimitingEnabled()` en `GenericRateLimitService`
- Modificar `canMakeRequest()` para verificar toggle
- Modificar `incrementRequest()` para verificar toggle

### **Fase 3: Testing**
- Probar con `RATE_LIMITING_ENABLED = "true"` (debe funcionar normal)
- Probar con `RATE_LIMITING_ENABLED = "false"` (debe permitir todo)
- Verificar que los logs muestran el estado correcto

### **Fase 4: Documentación**
- Documentar cómo usar el parámetro
- Agregar ejemplos de uso

---

## ⚠️ Advertencias

1. **Seguridad:** Deshabilitar rate limiting expone la aplicación a abusos. Solo hacerlo en casos justificados.

2. **Monitoreo:** Cuando esté deshabilitado, monitorear activamente el tráfico.

3. **Re-habilitación:** Recordar volver a habilitar después de la emergencia/mantenimiento.

4. **Valor por defecto:** Siempre debe ser `"true"` (habilitado) por seguridad.

---

## 📋 Checklist de Implementación

- [ ] Crear migración Flyway con el parámetro
- [ ] Agregar método `isRateLimitingEnabled()` en `GenericRateLimitService`
- [ ] Modificar `canMakeRequest()` para verificar toggle
- [ ] Modificar `incrementRequest()` para verificar toggle
- [ ] Agregar logs informativos
- [ ] Probar con toggle habilitado
- [ ] Probar con toggle deshabilitado
- [ ] Verificar que no hay degradación de performance
- [ ] Documentar uso del parámetro

---

¿Te parece bien esta solución? ¿Quieres que proceda con la implementación?

