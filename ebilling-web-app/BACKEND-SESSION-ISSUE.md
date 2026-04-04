# 🚨 Problema: Sesiones de Punto de Emisión se Pierden Inesperadamente

**Fecha:** 18 de Octubre 2025  
**Prioridad:** Alta  
**Microservicio Afectado:** `ebill-billing` (endpoint de heartbeat)

---

## 🔍 Síntoma Reportado

Los usuarios están siendo **deslogueados inesperadamente** mientras navegan activamente en la aplicación:

```
[DEBUG] 16:08:01 - 💓 Enviando heartbeat #5 a las 11:08:01 AM (última actividad hace 19s)
[ERROR] 16:08:01 - HTTP Error intercepted HttpErrorResponse {status: 404, url: 'http://localhost:8280/api/invoice-points/session/heartbeat'}
[ERROR] 16:08:01 - ❌ SESIÓN PERDIDA - Heartbeat #5 falló con 404
```

**Contexto:**

- El usuario estaba **activo hace solo 19 segundos**
- El heartbeat se envía cada **2 minutos**
- El backend retorna **404 (sesión no existe)**
- Esto fuerza un logout completo del usuario

---

## 🎯 Comportamiento Esperado vs. Actual

### Esperado:

1. Usuario asigna punto de emisión → Backend crea sesión
2. Frontend envía heartbeat cada 2 minutos (solo si usuario activo)
3. Backend mantiene sesión activa por 10 minutos desde último heartbeat
4. Si no hay heartbeat por 10 minutos → Backend libera el punto automáticamente
5. Usuario puede seguir trabajando sin interrupciones

### Actual:

1. Usuario asigna punto de emisión → Backend crea sesión ✅
2. Frontend envía heartbeat cada 2 minutos ✅
3. **Backend retorna 404 después de ~10 minutos** ❌
4. **Usuario es deslogueado aunque esté activo** ❌

---

## 🔎 Posibles Causas

### 1. **Redis expira tokens antes de tiempo**

- El JWT token se almacena en Redis con un TTL
- Si el TTL de Redis es menor que el timeout de sesión (10 min), el token expira antes
- **Verificar:** TTL del token en Redis vs. timeout de sesión del punto de emisión

### 2. **Sesión de punto de emisión se limpia incorrectamente**

- El backend puede estar limpiando sesiones activas por error
- **Verificar:** Lógica de limpieza de sesiones en `InvoicePointService`

### 3. **Múltiples instancias del backend sin sesiones compartidas**

- Si hay balanceo de carga, las sesiones pueden no estar sincronizadas
- **Verificar:** ¿Las sesiones de punto de emisión están en Redis o en memoria local?

### 4. **El endpoint de heartbeat no está actualizando el timestamp**

- El heartbeat llega al backend pero no actualiza la última actividad
- **Verificar:** Lógica del endpoint `POST /api/invoice-points/session/heartbeat`

---

## 📝 Información del Endpoint de Heartbeat

### Request:

```http
POST http://localhost:8280/api/invoice-points/session/heartbeat
Headers:
  Authorization: Bearer {JWT_TOKEN}
  Content-Type: application/json
```

### Response Esperada:

```http
HTTP 200 OK
{
  "message": "Heartbeat recibido",
  "lastActivity": "2025-10-18T11:08:01"
}
```

### Response Actual (problema):

```http
HTTP 404 Not Found
{
  "timestamp": "2025-10-18T11:08:01",
  "status": 404,
  "error": "Not Found",
  "message": "No active session found for this invoice point",
  "path": "/api/invoice-points/session/heartbeat"
}
```

---

## 🧪 Cómo Reproducir

1. Login como usuario con permisos de facturación
2. Seleccionar sucursal y punto de emisión en el flujo de bienvenida
3. Navegar a cualquier página (ej: Clientes, Productos)
4. **Esperar ~10 minutos navegando activamente**
5. Observar que el usuario es deslogueado inesperadamente

**Logs esperados en el backend:**

```
[INFO] Heartbeat recibido para punto de emisión ID: 1
[INFO] Sesión actualizada. Última actividad: 2025-10-18T11:08:01
```

**Logs actuales en el backend (problema):**

```
[WARN] Heartbeat recibido pero sesión no encontrada para punto de emisión ID: 1
[ERROR] No active session found
```

---

## 🔧 Solución Temporal Implementada en Frontend

Para mejorar la experiencia del usuario mientras se investiga el problema en el backend, se implementó:

### Cambio en `invoice-point-session.service.ts`:

**Antes:**

- Heartbeat falla con 404 → Logout inmediato del usuario

**Ahora:**

- Heartbeat falla con 404 → Verificar si usuario estaba activo
  - **Si activo (últimos 2 min):** Solo detener heartbeat, NO cerrar sesión completa
    - Mostrar mensaje: "La sesión del punto de emisión se perdió. Seleccione nuevamente el punto."
    - Limpiar solo `ID_INVOICE_POINT`, mantener token de login
  - **Si inactivo (>2 min):** Cerrar sesión completa como antes

**Beneficio:**

- Usuario activo NO es deslogueado completamente
- Solo necesita re-seleccionar el punto de emisión desde el menú
- Mantiene su sesión de login intacta

---

## ✅ Checklist de Verificación para Backend

### 1. Verificar TTL de Redis

```java
// En la configuración de Redis/JWT
// Verificar que el TTL del token sea MAYOR que el timeout de sesión

// Ejemplo:
private static final long JWT_TOKEN_VALIDITY = 24 * 60 * 60 * 1000; // 24 horas
private static final long SESSION_TIMEOUT = 10 * 60 * 1000; // 10 minutos

// JWT_TOKEN_VALIDITY DEBE SER > SESSION_TIMEOUT
```

### 2. Verificar Lógica del Endpoint de Heartbeat

```java
@PostMapping("/session/heartbeat")
public ResponseEntity<?> heartbeat(@RequestHeader("Authorization") String token) {
    // 1. Extraer el ID del punto de emisión del token o sesión
    Long invoicePointId = extractInvoicePointIdFromToken(token);

    // 2. Buscar la sesión activa
    InvoicePointSession session = sessionRepository.findByInvoicePointId(invoicePointId);

    if (session == null) {
        // ❌ PROBLEMA: Sesión no encontrada
        log.warn("Heartbeat recibido pero sesión no encontrada para punto: {}", invoicePointId);
        return ResponseEntity.status(404).body("No active session found");
    }

    // 3. Actualizar el timestamp de última actividad
    session.setLastActivity(LocalDateTime.now());
    sessionRepository.save(session);

    log.info("Heartbeat recibido para punto {}. Sesión actualizada.", invoicePointId);
    return ResponseEntity.ok("Heartbeat received");
}
```

### 3. Verificar Limpieza de Sesiones Expiradas

```java
// Scheduled task para limpiar sesiones expiradas
@Scheduled(fixedRate = 60000) // Cada 1 minuto
public void cleanExpiredSessions() {
    LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(10);

    List<InvoicePointSession> expiredSessions =
        sessionRepository.findByLastActivityBefore(expirationTime);

    for (InvoicePointSession session : expiredSessions) {
        log.info("Liberando punto de emisión {} por inactividad", session.getInvoicePointId());
        releaseInvoicePoint(session.getInvoicePointId());
        sessionRepository.delete(session);
    }
}
```

### 4. Verificar Almacenamiento de Sesiones

- ¿Las sesiones están en **Redis** (compartido entre instancias) o en **memoria local** (no compartido)?
- Si hay múltiples instancias del backend, las sesiones DEBEN estar en Redis

### 5. Logs de Debugging

Agregar logs detallados en el endpoint de heartbeat:

```java
log.debug("Heartbeat recibido. Token: {}, Punto: {}, Sesión existe: {}, Última actividad: {}",
    tokenHash, invoicePointId, session != null, session?.getLastActivity());
```

---

## 📊 Métricas a Monitorear

1. **Tasa de 404 en heartbeat:** Debería ser 0% para usuarios activos
2. **Tiempo promedio de sesión:** Debería ser > 10 minutos para usuarios activos
3. **Heartbeats exitosos vs. fallidos:** Ratio debería ser > 95% exitosos
4. **Sesiones liberadas por timeout vs. manual:** Mayoría debería ser manual

---

## 🚀 Próximos Pasos

1. **Backend:** Investigar logs del endpoint de heartbeat
2. **Backend:** Verificar TTL de Redis y lógica de sesiones
3. **Backend:** Agregar logs detallados para debugging
4. **DevOps:** Verificar si hay múltiples instancias sin Redis compartido
5. **Testing:** Reproducir el problema en ambiente de desarrollo con logs habilitados

---

## 📞 Contacto

Si necesitas más información o logs del frontend, contactar al equipo de frontend.

**Logs relevantes del frontend:**

- `invoice-point-session.service.ts` - Gestión de heartbeat
- `server-errors.interceptor.ts` - Interceptor de errores HTTP
- Console del navegador - Logs de debug con timestamps

---

**Fin del documento**
