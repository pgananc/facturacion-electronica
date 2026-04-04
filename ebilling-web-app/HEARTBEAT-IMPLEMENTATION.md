# 💓 Implementación de Heartbeat para Sesiones de Punto de Emisión

## 📋 Resumen

Se ha implementado un **sistema completo de heartbeat** para mantener activas las sesiones de punto de emisión y prevenir bloqueos por cierres abruptos de la aplicación.

### ✅ Características Implementadas

1. **Servicio Global de Sesión** (`InvoicePointSessionService`)

   - Maneja el ciclo completo de vida de las sesiones
   - Envía heartbeat automático cada 2 minutos
   - Limpia automáticamente recursos para evitar memory leaks
   - Detecta y notifica cuando la sesión expira (404)
   - **Redirige automáticamente** cuando detecta sesión expirada:
     - 404 (punto expirado) → Redirige a `/welcome` para reasignar punto
     - 401 (token expirado) → Redirige a `/login` para reautenticación

2. **Integración en Welcome Component**

   - Inicia heartbeat automáticamente al asignar punto de emisión
   - Inicia heartbeat al continuar con sesión existente

3. **Integración en Billing Component**
   - Simplificado para usar el servicio global
   - No maneja su propio heartbeat (evita duplicación)

---

## 🗂️ Archivos Modificados/Creados

### Nuevos Archivos

#### `src/app/_service/auth/invoice-point-session.service.ts`

Servicio singleton que gestiona sesiones con heartbeat automático.

**Métodos principales:**

- `startHeartbeat()` - Inicia envío periódico de heartbeat
- `stopHeartbeat()` - Detiene el heartbeat
- `assignInvoicePointWithHeartbeat(id)` - Asigna punto + inicia heartbeat
- `releaseInvoicePointWithHeartbeat()` - Detiene heartbeat + libera punto
- `isActive()` - Verifica si hay heartbeat activo

### Archivos Modificados

#### `src/app/_service/auth/invoice-point.service.ts`

Agregados métodos para interactuar con el backend:

- `releaseInvoicePoint()` - DELETE /api/invoice-points/session
- `sendHeartbeat()` - POST /api/invoice-points/session/heartbeat

#### `src/app/pages/welcome/welcome.component.ts`

- Usa `InvoicePointSessionService` en lugar de `InvoicePointService` directamente
- Inicia heartbeat automáticamente al asignar punto
- Inicia heartbeat para sesiones existentes

#### `src/app/pages/billing/billing.component.ts`

- Implementa `OnDestroy` para limpieza
- Confía en el servicio global para el heartbeat (no maneja su propio intervalo)

#### `src/app/_service/auth/login.service.ts`

- Integrado `releaseInvoicePointWithHeartbeat()` en el método `closeSession()`
- Libera automáticamente el punto de emisión al cerrar sesión
- Maneja errores gracefully (continúa con logout aunque falle la liberación)

---

## 🚀 Cómo Funciona

### Flujo de Asignación de Punto de Emisión

```
1. Usuario selecciona punto de emisión en Welcome Component
   ↓
2. WelcomeComponent.assignInvoicePoint() se ejecuta
   ↓
3. Se guarda información en sessionStorage
   ↓
4. InvoicePointSessionService.assignInvoicePointWithHeartbeat()
   ↓
5. Backend asigna el punto (POST /api/invoice-points/session)
   ↓
6. Heartbeat se inicia automáticamente (envío cada 2 min)
   ↓
7. Usuario puede usar la aplicación normalmente
```

### Flujo de Heartbeat

```
Cada 2 minutos:
  ↓
InvoicePointSessionService envía POST /api/invoice-points/session/heartbeat
  ↓
Backend actualiza last_activity de la sesión
  ↓
Si respuesta = 200 OK: Todo bien, continuar
Si respuesta = 404: Sesión expiró, detener heartbeat y notificar usuario
Si respuesta = 401: Token inválido, detener heartbeat
Otro error: Problema temporal, seguir intentando
```

### Flujo de Liberación Automática (Backend)

```
Cada 3 minutos (tarea programada en backend):
  ↓
Busca sesiones con last_activity > 10 minutos
  ↓
Marca sesiones inactivas como active = false
  ↓
Limpia registros en Redis
  ↓
Registra en logs qué sesiones se liberaron
```

---

## 🔧 Configuración

### Backend (application.properties)

```properties
# Timeout de inactividad - sesión se libera si no hay heartbeat por 10 min
session.inactivity.timeout.minutes=10

# Frecuencia de limpieza automática - cada 3 minutos
session.cleanup.cron=0 */3 * * * *
```

### Frontend (TypeScript)

```typescript
// En InvoicePointSessionService
private readonly HEARTBEAT_INTERVAL_MS = 2 * 60 * 1000; // 2 minutos
```

**¿Por qué 2 minutos?**

- Timeout del servidor: 10 minutos
- Heartbeat cada 2 minutos = 5 heartbeats en 10 minutos
- Si hay problemas de red temporales (1-2 min), la sesión no se pierde
- Balance entre tráfico de red y seguridad de la sesión

---

## 📝 Casos de Uso y Comportamiento

### ✅ Caso 1: Usuario trabajando normalmente

**Escenario:**

1. Usuario asigna punto de emisión
2. Trabaja durante 30 minutos
3. Libera el punto manualmente

**Resultado:**

- ✅ Heartbeat se envía cada 2 minutos automáticamente
- ✅ Sesión se mantiene activa todo el tiempo
- ✅ Al liberar, heartbeat se detiene y punto queda disponible

### ✅ Caso 2: Usuario cierra el navegador abruptamente

**Escenario:**

1. Usuario asigna punto y trabaja 5 minutos
2. Cierra el navegador sin liberar el punto
3. Heartbeats dejan de llegar al servidor

**Resultado:**

- ✅ Después de 10 minutos sin heartbeat
- ✅ Tarea programada detecta inactividad
- ✅ Punto se libera automáticamente
- ⏱️ Tiempo total: ~10-13 minutos (10 min timeout + hasta 3 min de la tarea)
- ✅ Si el usuario vuelve a abrir la app, se redirige automáticamente a `/welcome` para reasignar punto

### ✅ Caso 3: Aplicación crashea

**Escenario:**

1. Usuario asigna punto
2. App crashea por error fatal
3. JavaScript deja de ejecutarse

**Resultado:**

- ✅ Heartbeat se detiene inmediatamente
- ✅ Después de 10 minutos, servidor libera el punto automáticamente
- ✅ Usuario puede volver a asignar el punto al reiniciar

### ✅ Caso 4: Pérdida temporal de conexión

**Escenario:**

1. Usuario asigna punto y trabaja
2. Pierde conexión a internet por 5 minutos
3. Recupera conexión

**Resultado:**

- ⚠️ Heartbeats fallan durante la desconexión
- ✅ Si recupera conexión antes de 10 min, próximo heartbeat exitoso mantiene la sesión
- ❌ Si supera 10 min desconectado, sesión se libera automáticamente

### ⚠️ Caso 5: Conexión intermitente (mala calidad)

**Escenario:**

1. Usuario asigna punto
2. Conexión inestable: heartbeats fallan ocasionalmente
3. Si pasan más de 10 min sin un heartbeat exitoso

**Resultado:**

- ❌ Servidor libera el punto después de 10 min sin heartbeat exitoso
- ✅ Frontend detecta 404 en próximo heartbeat exitoso
- ✅ Usuario es redirigido automáticamente a `/welcome` para reasignar punto
- 💡 **Recomendación:** En ambientes con mala conectividad, considerar reducir el intervalo a 1.5 minutos

### ✅ Caso 6: Usuario vuelve después de sesión expirada

**Escenario:**

1. Usuario asignó punto y cerró navegador
2. Pasaron más de 10 minutos (sesión expiró en backend)
3. Usuario vuelve a abrir la aplicación
4. sessionStorage aún tiene datos del punto anterior

**Resultado:**

- ✅ Heartbeat se inicia con datos del sessionStorage
- ✅ Backend responde 404 (sesión no existe)
- ✅ Sistema detecta sesión expirada
- ✅ Usuario ve notificación: "Su sesión de punto de emisión ha expirado. Redirigiendo..."
- ✅ Usuario es redirigido automáticamente a `/welcome` en 1.5 segundos
- ✅ Usuario selecciona nuevo punto de emisión
- ✅ Nuevo heartbeat inicia automáticamente

---

## 🧑‍💻 Uso para Desarrolladores

### Asignar Punto de Emisión con Heartbeat Automático

```typescript
// En cualquier componente/servicio
constructor(
  private invoicePointSessionService: InvoicePointSessionService
) {}

// Opción 1: Asignar + iniciar heartbeat en un solo paso (RECOMENDADO)
await this.invoicePointSessionService
  .assignInvoicePointWithHeartbeat(idPuntoEmision);

// Opción 2: Control manual
// Paso 1: Asignar punto
await this.invoicePointService.assignInvoicePoint(idPuntoEmision).toPromise();

// Paso 2: Iniciar heartbeat manualmente
this.invoicePointSessionService.startHeartbeat();
```

### Liberar Punto de Emisión

```typescript
// Opción 1: Liberar + detener heartbeat en un solo paso (RECOMENDADO)
await this.invoicePointSessionService.releaseInvoicePointWithHeartbeat();

// Opción 2: Control manual
// Paso 1: Detener heartbeat
this.invoicePointSessionService.stopHeartbeat();

// Paso 2: Liberar punto
await this.invoicePointService.releaseInvoicePoint().toPromise();
```

### Verificar Estado del Heartbeat

```typescript
if (this.invoicePointSessionService.isActive()) {
  console.log("✅ Heartbeat está activo");
} else {
  console.log("⚠️  Heartbeat NO está activo");
}
```

---

## ✅ Integración en Logout

**✅ COMPLETADO: Ya está integrado en el flujo de logout**

El método `closeSession()` en `LoginService` automáticamente:

1. Libera el punto de emisión
2. Detiene el heartbeat
3. Realiza logout del servidor
4. Limpia el sessionStorage
5. Redirige al usuario a la página de login

Si la liberación del punto falla (por ejemplo, por problemas de red), el logout continúa de todas formas para no bloquear al usuario.

**Código implementado en `login.service.ts`:**

```typescript
closeSession() {
  const userName = decodeToken().userName;

  // Primero liberar punto de emisión y detener heartbeat
  this.invoicePointSessionService
    .releaseInvoicePointWithHeartbeat()
    .then(() => {
      console.log('✅ Punto de emisión liberado correctamente durante logout');
    })
    .catch((err) => {
      // Si falla la liberación del punto, continuar con logout de todas formas
      console.warn('⚠️  Error al liberar punto de emisión durante logout:', err);
    })
    .finally(() => {
      // Realizar logout del servidor (siempre se ejecuta)
      this.logoutFromServer(userName).subscribe({
        next: () => {
          sessionStorage.clear();
          this.setLoginStatus(false);
          this.router.navigate(['login']);
        },
        error: () => {
          // Incluso si falla el logout del servidor, igual cerramos sesión localmente
          sessionStorage.clear();
          this.setLoginStatus(false);
          this.router.navigate(['login']);
        },
      });
    });
}
```

---

## 🧪 Testing Manual

### Test 1: Verificar que el heartbeat se inicia

1. Iniciar sesión en la aplicación
2. Seleccionar compañía y punto de emisión
3. Abrir DevTools → Console
4. Buscar logs: `💓 Heartbeat iniciado (cada 2 minutos)`
5. Esperar 2 minutos
6. Verificar log: `💓 Heartbeat enviado exitosamente`

### Test 2: Verificar liberación automática

1. Asignar un punto de emisión
2. Cerrar el navegador abruptamente (sin liberar)
3. Esperar 11-13 minutos
4. Revisar logs del backend: debe aparecer la liberación automática
5. Intentar asignar el mismo punto desde otro usuario → Debe estar disponible

### Test 3: Verificar notificación de expiración

1. Asignar punto de emisión
2. En otro navegador/incógnito, forzar liberación del punto (backend o base de datos)
3. Esperar hasta 2 minutos (próximo heartbeat)
4. Verificar que aparezca notificación: "Su sesión de punto de emisión ha expirado"

---

## 📊 Logs y Monitoreo

### Logs del Frontend (Console)

```
💓 InvoicePointSessionService inicializado
💓 Heartbeat iniciado (cada 2 minutos)
💓 Heartbeat enviado exitosamente
⚠️  No hay sesión activa, deteniendo heartbeat
🛑 Heartbeat detenido
❌ Error al enviar heartbeat (problema temporal): 500
```

### Logs del Backend

```
[INFO] 🧹 Iniciando limpieza de sesiones inactivas...
[INFO] ⚠️  Encontradas 2 sesiones inactivas
[INFO] ✅ Sesión liberada - Usuario: jguerrero - Punto: 123 - Última actividad: 2025-10-12T09:48:00
[INFO] ✅ Limpieza completada: 2 de 2 sesiones liberadas
```

---

## 🔍 Troubleshooting

### Problema: Heartbeat no se inicia

**Síntomas:**

- No aparece log "💓 Heartbeat iniciado"
- Sesión se expira aunque el usuario esté activo

**Posibles causas:**

1. No hay punto de emisión en `sessionStorage` → Verificar `ID_INVOICE_POINT`
2. El servicio no se está inyectando correctamente
3. `assignInvoicePointWithHeartbeat()` no se está llamando

**Solución:**

```typescript
// Verificar en DevTools → Application → Session Storage
sessionStorage.getItem('idInvoicePoint'); // Debe tener un valor

// Verificar que el servicio se inyecte en el componente
constructor(
  private invoicePointSessionService: InvoicePointSessionService
) {}
```

### Problema: Múltiples heartbeats activos

**Síntomas:**

- Logs duplicados: "💓 Heartbeat enviado exitosamente" aparece 2+ veces cada 2 min
- Network tab muestra múltiples llamadas simultáneas

**Causa:**

- Se está llamando a `startHeartbeat()` múltiples veces sin detener el anterior

**Solución:**

- El servicio ya tiene protección contra esto
- Verificar que no se esté creando múltiples instancias del servicio (debe ser singleton)

### Problema: Sesión se libera antes de 10 minutos

**Síntomas:**

- Usuario reporta que su sesión expira muy rápido
- Heartbeat se envía, pero el servidor responde 404

**Posibles causas:**

1. Configuración del backend con timeout muy corto
2. Zona horaria del servidor vs. cliente
3. Backend no está actualizando `last_activity` correctamente

**Verificación en Backend:**

```sql
-- Ver sesiones activas y su última actividad
SELECT
  u.user_name,
  s.id_invoice_issuance_point,
  s.last_activity,
  EXTRACT(EPOCH FROM (NOW() - s.last_activity))/60 as minutes_inactive,
  s.active
FROM user_invoice_point_session s
JOIN users u ON s.id_user = u.id_user
WHERE s.active = true;
```

---

## 📚 Referencias

- **Guía del servidor:** Ver archivo raíz del proyecto para documentación completa del backend
- **Endpoint de heartbeat:** `POST /api/invoice-points/session/heartbeat`
- **Endpoint de liberación:** `DELETE /api/invoice-points/session`
- **Endpoint de asignación:** `POST /api/invoice-points/session`

---

## ✨ Próximos Pasos (Opcionales)

- [x] ~~Integrar `releaseInvoicePointWithHeartbeat()` en el flujo de logout~~ **✅ COMPLETADO**
- [ ] Agregar tests unitarios para `InvoicePointSessionService`
- [ ] Considerar agregar un indicador visual de "sesión activa" en la UI
- [ ] Implementar métricas de heartbeat (éxitos/fallos) para monitoreo
- [ ] Agregar retry automático para heartbeats fallidos (ej: 3 reintentos con backoff exponencial)

---

**Última actualización:** Octubre 2025  
**Autor:** Sistema de Facturación Electrónica  
**Versión:** 1.0.0
