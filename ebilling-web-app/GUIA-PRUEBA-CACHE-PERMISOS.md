# 🧪 Guía de Pruebas: Cache de Validaciones de Features

## 📋 Resumen

Esta guía explica cómo probar el sistema de cache de validaciones de features implementado en `PermissionService`.

---

## 🎯 Objetivos de las Pruebas

1. ✅ Verificar que el cache funciona correctamente (reduce llamadas HTTP)
2. ✅ Verificar que el cache se guarda en memoria y localStorage
3. ✅ Verificar que el cache se invalida cuando cambian permisos
4. ✅ Verificar que el cache expira correctamente después de 5 minutos

---

## 🧪 Pruebas Automatizadas (Tests Unitarios)

### Tests Implementados

Los tests están en `src/app/_service/permission/permission.service.spec.ts`:

#### 1. **Cache Hit en Memoria**
```typescript
it('should cache validation result and return from cache on second call')
```
- **Qué prueba:** Que la segunda llamada a `canAccess()` no hace HTTP request
- **Resultado esperado:** Solo 1 HTTP request, segunda llamada usa cache

#### 2. **Cache Hit en localStorage**
```typescript
it('should use localStorage cache when memory cache is empty')
```
- **Qué prueba:** Que el cache persiste entre recargas de página
- **Resultado esperado:** No hace HTTP request si hay cache válido en localStorage

#### 3. **Invalidación de Cache Específica**
```typescript
it('should invalidate cache for specific feature')
```
- **Qué prueba:** Que `invalidateFeatureCache(featureCode)` limpia solo esa feature
- **Resultado esperado:** Después de invalidar, hace HTTP request nuevamente

#### 4. **Invalidación de Todo el Cache**
```typescript
it('should invalidate all cache when no feature specified')
```
- **Qué prueba:** Que `invalidateFeatureCache()` sin parámetros limpia todo
- **Resultado esperado:** Todas las features hacen HTTP request después de invalidar

#### 5. **Guardado en Cache**
```typescript
it('should save to cache after successful backend response')
```
- **Qué prueba:** Que el resultado se guarda en localStorage después de respuesta exitosa
- **Resultado esperado:** Cache guardado con `result`, `timestamp`, y `ttl`

#### 6. **Cache en canAccessWithDetails**
```typescript
it('should use cache for canAccessWithDetails when available')
```
- **Qué prueba:** Que `canAccessWithDetails()` también usa cache
- **Resultado esperado:** No hace HTTP request si hay cache disponible

### Ejecutar Tests

```bash
# Ejecutar solo los tests de PermissionService
npm test -- --include='**/permission.service.spec.ts'

# Ejecutar con cobertura
npm test -- --include='**/permission.service.spec.ts' --code-coverage
```

---

## 🔍 Pruebas Manuales (DevTools)

### Paso 1: Preparar el Ambiente

1. Abrir la aplicación en el navegador
2. Abrir DevTools (F12)
3. Ir a la pestaña **Network**
4. Filtrar por `validate-feature`

### Paso 2: Verificar Cache Hit en Memoria

**Escenario:** Navegar a una ruta protegida dos veces

1. **Primera navegación:**
   - Navegar a `/products` (requiere `PRODUCT_MANAGEMENT`)
   - En Network, verificar que aparece **1 request** a `/api/permission/validate-feature`
   - Request payload: `{ featureCode: "PRODUCT_MANAGEMENT", idCompany: 1 }`

2. **Segunda navegación (inmediata):**
   - Navegar a otra página (ej: `/clients`)
   - Volver a `/products`
   - En Network, verificar que **NO aparece nuevo request** a `validate-feature`
   - ✅ **Cache hit exitoso**

**Logs esperados en Console:**
```
[PermissionService] ✅ Cache hit (memoria) para PRODUCT_MANAGEMENT-1
```

### Paso 3: Verificar Cache en localStorage

**Escenario:** Recargar la página y verificar que usa cache

1. **Antes de recargar:**
   - Navegar a `/products`
   - Abrir DevTools → Application → Local Storage
   - Buscar clave: `PERMISSION_CACHE_PRODUCT_MANAGEMENT-1`
   - Verificar que existe con estructura:
     ```json
     {
       "result": true,
       "timestamp": 1234567890,
       "ttl": 300000
     }
     ```

2. **Después de recargar:**
   - Recargar la página (F5)
   - Navegar a `/products` nuevamente
   - En Network, verificar que **NO aparece request** a `validate-feature`
   - ✅ **Cache de localStorage funcionando**

**Logs esperados en Console:**
```
[PermissionService] ✅ Cache hit (localStorage) para PRODUCT_MANAGEMENT-1
```

### Paso 4: Verificar Invalidación de Cache

**Escenario:** Cambiar permisos y verificar que se invalida el cache

1. **Como super_admin:**
   - Ir a `/admin/role-permissions`
   - Cambiar permisos de un rol (ej: quitar `PRODUCT_MANAGEMENT` del rol `admin`)
   - Guardar cambios

2. **Verificar invalidación:**
   - Abrir DevTools → Application → Local Storage
   - Verificar que `PERMISSION_CACHE_PRODUCT_MANAGEMENT-1` fue eliminado
   - ✅ **Cache invalidado correctamente**

3. **Verificar nueva validación:**
   - Navegar a `/products` como usuario afectado
   - En Network, verificar que aparece **nuevo request** a `validate-feature`
   - ✅ **Cache se regenera después de invalidar**

**Logs esperados en Console:**
```
[PermissionService] Cache invalidado para feature: PRODUCT_MANAGEMENT
```

### Paso 5: Verificar Expiración de Cache

**Escenario:** Esperar 5 minutos y verificar que el cache expira

**Opción A: Modificar TTL temporalmente (para pruebas rápidas)**

1. En `permission.service.ts`, cambiar temporalmente:
   ```typescript
   private readonly CACHE_TTL = 10000; // 10 segundos para pruebas
   ```

2. Navegar a `/products` → Se guarda en cache
3. Esperar 11 segundos
4. Navegar a `/products` nuevamente
5. En Network, verificar que aparece **nuevo request**
6. ✅ **Cache expirado correctamente**

**Opción B: Modificar timestamp manualmente (para pruebas inmediatas)**

1. Abrir DevTools → Application → Local Storage
2. Buscar `PERMISSION_CACHE_PRODUCT_MANAGEMENT-1`
3. Modificar `timestamp` a un valor antiguo:
   ```json
   {
     "result": true,
     "timestamp": 1000000000,  // ← Valor muy antiguo
     "ttl": 300000
   }
   ```
4. Navegar a `/products`
5. En Network, verificar que aparece **nuevo request**
6. ✅ **Cache expirado detectado**

---

## 📊 Métricas de Rendimiento

### Antes del Cache

```
Navegación típica:
- /products → 1 HTTP request
- /clients → 1 HTTP request
- /users → 1 HTTP request
- Vuelve a /products → 1 HTTP request (repetido!)
- Menú lateral carga → 5 HTTP requests
Total: 9 requests en 30 segundos
```

### Después del Cache

```
Navegación típica:
- /products → 1 HTTP request → Guarda en cache
- /clients → 1 HTTP request → Guarda en cache
- /users → 1 HTTP request → Guarda en cache
- Vuelve a /products → ✅ Cache hit (sin request)
- Menú lateral carga → ✅ Cache hits (sin requests)
Total: 3 requests en 30 segundos (66% reducción!)
```

---

## 🐛 Debugging

### Ver Estado del Cache

**En DevTools Console:**
```javascript
// Ver todas las claves de cache
Object.keys(localStorage).filter(k => k.startsWith('PERMISSION_CACHE_'))

// Ver cache específico
JSON.parse(localStorage.getItem('PERMISSION_CACHE_PRODUCT_MANAGEMENT-1'))

// Limpiar todo el cache manualmente
Object.keys(localStorage)
  .filter(k => k.startsWith('PERMISSION_CACHE_'))
  .forEach(k => localStorage.removeItem(k))
```

### Logs Útiles

El servicio tiene logs detallados. Buscar en Console:

- `✅ Cache hit (memoria)` - Cache encontrado en memoria
- `✅ Cache hit (localStorage)` - Cache encontrado en localStorage
- `Cache invalidado para feature: X` - Cache invalidado específicamente
- `Todo el cache de validaciones invalidado` - Todo el cache limpiado

---

## ✅ Checklist de Verificación

### Funcionalidad Básica
- [ ] Primera llamada hace HTTP request
- [ ] Segunda llamada usa cache (sin HTTP request)
- [ ] Cache se guarda en localStorage
- [ ] Cache persiste después de recargar página

### Invalidación
- [ ] `invalidateFeatureCache(featureCode)` limpia feature específica
- [ ] `invalidateFeatureCache()` limpia todo el cache
- [ ] Cambiar permisos invalida cache automáticamente
- [ ] Refrescar suscripción invalida cache automáticamente

### Expiración
- [ ] Cache expira después de 5 minutos
- [ ] Cache expirado se limpia automáticamente
- [ ] Después de expirar, hace nuevo HTTP request

### Edge Cases
- [ ] Super admin no usa cache (siempre permite acceso)
- [ ] Sin token no hace request ni guarda cache
- [ ] Sin idCompany no hace request ni guarda cache
- [ ] Error del backend no guarda en cache

---

## 🎯 Casos de Uso Reales

### Caso 1: Usuario Navegando Normalmente

```
1. Login → Navega a /products → HTTP request #1
2. Navega a /clients → HTTP request #2
3. Vuelve a /products → ✅ Cache hit (sin request)
4. Recarga página → ✅ Cache de localStorage (sin request)
5. Espera 6 minutos → Cache expira
6. Navega a /products → HTTP request #3 (nuevo)
```

### Caso 2: Admin Cambiando Permisos

```
1. Admin cambia permisos de rol "admin"
2. Sistema invalida cache automáticamente
3. Usuario afectado navega a /products → HTTP request (nuevo)
4. Cache se regenera con nuevos permisos
```

### Caso 3: Múltiples Features en Menú

```
1. Menú lateral carga → Valida 8 features
   - Sin cache: 8 HTTP requests
   - Con cache: 0-2 HTTP requests (solo las nuevas)
2. Usuario navega por la app
   - Todas las validaciones usan cache
   - Sin requests adicionales
```

---

## 📈 Resultados Esperados

### Reducción de Requests HTTP

| Escenario | Sin Cache | Con Cache | Reducción |
|-----------|-----------|-----------|-----------|
| Navegación básica (5 rutas) | 5 requests | 1-2 requests | 60-80% |
| Carga de menú lateral | 8 requests | 0-2 requests | 75-100% |
| Navegación repetida | 10 requests | 2-3 requests | 70-80% |

### Mejora de Performance

- **Tiempo de respuesta:** De ~200ms (HTTP) a ~1ms (cache)
- **Carga del servidor:** Reducción del 60-80% en requests de validación
- **Experiencia de usuario:** Navegación más fluida, sin delays

---

## 🔧 Troubleshooting

### Problema: Cache no funciona

**Síntomas:**
- Siempre hace HTTP request, incluso en segunda llamada

**Solución:**
1. Verificar que `idCompany` está en localStorage
2. Verificar que el token es válido
3. Revisar logs en Console para errores
4. Verificar que no es super_admin (super_admin no usa cache)

### Problema: Cache no se invalida

**Síntomas:**
- Cambios de permisos no se reflejan inmediatamente

**Solución:**
1. Verificar que `PermissionMatrixComponent` llama `invalidateFeatureCache()`
2. Verificar que se está pasando el `featureCode` correcto
3. Limpiar cache manualmente desde DevTools si es necesario

### Problema: Cache expira muy rápido

**Síntomas:**
- Cache se regenera constantemente

**Solución:**
1. Verificar que `CACHE_TTL` es 300000 (5 minutos)
2. Verificar que `timestamp` se guarda correctamente
3. Revisar si hay código que invalida cache innecesariamente

---

**Última actualización:** Enero 2026  
**Versión:** 1.0

