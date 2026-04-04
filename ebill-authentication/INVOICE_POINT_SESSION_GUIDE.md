# 📝 Guía de Gestión de Sesiones de Punto de Emisión

## 🎯 Problema y Solución

### El Problema
Cuando un usuario asigna un punto de emisión pero:
- Se cierra abruptamente la aplicación
- Se cierra el navegador
- Pierde conexión a internet
- La app se cae/crashea

El punto de emisión quedaba **bloqueado** (`active = true`) y no se liberaba automáticamente.

### La Solución
Sistema de **heartbeat + limpieza automática** que:
1. El cliente envía señales periódicas de "estoy vivo" (heartbeat)
2. El servidor revisa cada 3 minutos las sesiones inactivas
3. Si una sesión lleva **más de 10 minutos** sin heartbeat, se libera automáticamente

---

## 🔧 Configuración del Servidor

### Parámetros Configurables

En `application.properties`:

```properties
# Timeout de inactividad (10 minutos)
session.inactivity.timeout.minutes=10

# Frecuencia de limpieza automática (cada 3 minutos)
session.cleanup.cron=0 */3 * * * *
```

### ¿Qué hace el servidor?

1. **Cada 3 minutos** ejecuta una tarea programada
2. Busca sesiones activas sin actividad en los últimos 10 minutos
3. Las marca como `active = false`
4. Establece `end_time = now`
5. Limpia el registro en Redis
6. Registra en logs qué sesiones se liberaron

---

## 💻 Implementación en el Cliente (Frontend)

### Endpoint de Heartbeat

```http
POST /api/invoice-points/session/heartbeat
Authorization: Bearer {token}
```

**Respuestas:**
- `200 OK` - Heartbeat recibido, sesión actualizada
- `404 Not Found` - No hay sesión activa para este usuario
- `401 Unauthorized` - Token inválido o expirado

### Ejemplo de Implementación

#### React / Angular / Vue (JavaScript/TypeScript)

```javascript
// Servicio de sesión
class InvoicePointSessionService {
  private heartbeatInterval: number | null = null;
  
  /**
   * Inicia el heartbeat automático
   * Se debe llamar inmediatamente después de asignar un punto de emisión
   */
  startHeartbeat() {
    // Enviar heartbeat cada 2 minutos (120,000 ms)
    // El timeout del servidor es 10 min, así que con 2 min estamos seguros
    this.heartbeatInterval = setInterval(() => {
      this.sendHeartbeat();
    }, 2 * 60 * 1000); // 2 minutos
    
    console.log('💓 Heartbeat iniciado (cada 2 minutos)');
  }
  
  /**
   * Envía heartbeat al servidor
   */
  async sendHeartbeat() {
    try {
      const response = await fetch('/api/invoice-points/session/heartbeat', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.getToken()}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        console.log('💓 Heartbeat enviado exitosamente');
      } else if (response.status === 404) {
        console.warn('⚠️  No hay sesión activa, deteniendo heartbeat');
        this.stopHeartbeat();
      } else {
        console.error('❌ Error al enviar heartbeat:', response.status);
      }
    } catch (error) {
      console.error('❌ Error de red al enviar heartbeat:', error);
      // NO detenemos el heartbeat aquí - puede ser un problema temporal de red
    }
  }
  
  /**
   * Detiene el heartbeat
   * Se debe llamar cuando el usuario libera el punto o cierra sesión
   */
  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
      console.log('🛑 Heartbeat detenido');
    }
  }
  
  /**
   * Asigna un punto de emisión e inicia heartbeat
   */
  async assignInvoicePoint(idInvoiceIssuancePoint: number) {
    try {
      const response = await fetch('/api/invoice-points/session', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.getToken()}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ idInvoiceIssuancePoint })
      });
      
      if (response.ok) {
        console.log('✅ Punto de emisión asignado');
        // IMPORTANTE: Iniciar heartbeat inmediatamente después de asignar
        this.startHeartbeat();
        return true;
      }
      return false;
    } catch (error) {
      console.error('❌ Error al asignar punto de emisión:', error);
      return false;
    }
  }
  
  /**
   * Libera el punto de emisión y detiene heartbeat
   */
  async releaseInvoicePoint() {
    // Primero detener heartbeat
    this.stopHeartbeat();
    
    try {
      const response = await fetch('/api/invoice-points/session', {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });
      
      if (response.ok) {
        console.log('✅ Punto de emisión liberado');
        return true;
      }
      return false;
    } catch (error) {
      console.error('❌ Error al liberar punto de emisión:', error);
      return false;
    }
  }
  
  private getToken(): string {
    // Implementar según tu sistema de auth
    return localStorage.getItem('token') || '';
  }
}

// Uso en componente
const sessionService = new InvoicePointSessionService();

// Al asignar punto
await sessionService.assignInvoicePoint(123);

// Al liberar punto (cuando el usuario termina o cierra sesión)
await sessionService.releaseInvoicePoint();
```

#### React Hook Personalizado

```typescript
import { useEffect, useRef } from 'react';

export const useInvoicePointSession = (isActive: boolean) => {
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  
  const sendHeartbeat = async () => {
    try {
      const response = await fetch('/api/invoice-points/session/heartbeat', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        }
      });
      
      if (!response.ok && response.status === 404) {
        // Sesión no encontrada, detener heartbeat
        stopHeartbeat();
      }
    } catch (error) {
      console.error('Error sending heartbeat:', error);
    }
  };
  
  const startHeartbeat = () => {
    if (intervalRef.current) return; // Ya está iniciado
    
    intervalRef.current = setInterval(sendHeartbeat, 2 * 60 * 1000); // 2 min
    console.log('💓 Heartbeat started');
  };
  
  const stopHeartbeat = () => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
      console.log('🛑 Heartbeat stopped');
    }
  };
  
  useEffect(() => {
    if (isActive) {
      startHeartbeat();
    } else {
      stopHeartbeat();
    }
    
    // Cleanup al desmontar
    return () => stopHeartbeat();
  }, [isActive]);
  
  return { startHeartbeat, stopHeartbeat };
};

// Uso en componente
function InvoiceComponent() {
  const [hasActivePoint, setHasActivePoint] = useState(false);
  useInvoicePointSession(hasActivePoint);
  
  // ... resto del componente
}
```

---

## 🎯 Casos de Uso

### ✅ Caso 1: Usuario trabajando normalmente

1. Usuario asigna punto de emisión
2. Frontend inicia heartbeat cada 2 minutos
3. Backend recibe heartbeats y actualiza `last_activity`
4. Usuario trabaja durante horas sin problemas
5. Usuario libera el punto manualmente
6. Frontend detiene heartbeat

**Resultado:** ✅ Todo funciona correctamente

### ✅ Caso 2: Usuario cierra el navegador abruptamente

1. Usuario asigna punto de emisión
2. Frontend inicia heartbeat cada 2 minutos
3. Usuario cierra el navegador sin liberar
4. Heartbeats dejan de llegar
5. Después de 10 minutos sin heartbeat
6. El servidor automáticamente libera el punto (tarea cada 3 min)

**Resultado:** ✅ Punto liberado automáticamente en ~10-13 minutos

### ✅ Caso 3: App se crashea

1. Usuario asigna punto de emisión
2. Frontend inicia heartbeat
3. App crashea / error fatal
4. Heartbeats dejan de llegar
5. Servidor detecta inactividad > 10 min
6. Libera automáticamente el punto

**Resultado:** ✅ Punto liberado automáticamente

### ✅ Caso 4: Usuario pierde conexión a internet

1. Usuario asigna punto y trabaja
2. Pierde conexión temporalmente (5 min)
3. Heartbeats fallan pero el intervalo sigue intentando
4. Recupera conexión antes de los 10 min
5. Próximo heartbeat exitoso actualiza `last_activity`

**Resultado:** ✅ Sesión se mantiene activa

### ⚠️ Caso 5: Usuario con mala conexión (intermitente)

1. Usuario asigna punto
2. Conexión inestable: heartbeats fallan ocasionalmente
3. Si pasan más de 10 min sin un heartbeat exitoso
4. Servidor libera el punto
5. Usuario ve error al intentar facturar

**Solución:** Implementar retry en heartbeat y/o reducir intervalo a 1.5 min

---

## 📊 Logs y Monitoreo

### Logs del Servidor

El servidor registra información útil:

```log
2025-10-12 10:00:00 INFO  🧹 Iniciando limpieza de sesiones inactivas...
2025-10-12 10:00:01 INFO  ⚠️  Encontradas 2 sesiones inactivas
2025-10-12 10:00:01 INFO  ✅ Sesión liberada - Usuario: jguerrero - Punto: 123 - Última actividad: 2025-10-12T09:48:00
2025-10-12 10:00:01 INFO  ✅ Sesión liberada - Usuario: mrodriguez - Punto: 456 - Última actividad: 2025-10-12T09:45:00
2025-10-12 10:00:01 INFO  ✅ Limpieza completada: 2 de 2 sesiones liberadas
```

### Logs del Cliente (Recomendado)

```javascript
// Implementar logs en producción para debugging
console.log('💓 Heartbeat enviado exitosamente');
console.warn('⚠️  No hay sesión activa, deteniendo heartbeat');
console.error('❌ Error al enviar heartbeat');
```

---

## ⚙️ Configuraciones Recomendadas

### Producción

```properties
# Timeout: 10 minutos (balance entre UX y recursos)
session.inactivity.timeout.minutes=10

# Limpieza: cada 3 minutos (responsivo pero no excesivo)
session.cleanup.cron=0 */3 * * * *
```

**Frontend:** Heartbeat cada **2 minutos**

### Desarrollo/Testing

```properties
# Timeout más corto para testing
session.inactivity.timeout.minutes=5

# Limpieza más frecuente
session.cleanup.cron=0 */1 * * * *
```

**Frontend:** Heartbeat cada **1 minuto**

---

## 🚨 Consideraciones Importantes

### ⚠️ NO olvidar:

1. **Siempre iniciar heartbeat** inmediatamente después de asignar punto
2. **Siempre detener heartbeat** al liberar el punto manualmente
3. **Detener heartbeat** cuando el usuario cierre sesión
4. **Limpiar intervalos** en componentes desmontados (React/Angular/Vue)

### ⚠️ Errores comunes:

❌ **No iniciar heartbeat** después de asignar  
❌ **Memory leaks** por no limpiar intervalos  
❌ **Múltiples intervalos** por iniciar sin detener el anterior  
❌ **Intervalo muy largo** (> 5 min) arriesga pérdida de sesión  
❌ **Intervalo muy corto** (< 1 min) genera tráfico innecesario  

---

## 🧪 Testing

### Test Manual en Browser Console

```javascript
// 1. Asignar punto
await fetch('/api/invoice-points/session', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ idInvoiceIssuancePoint: 123 })
});

// 2. Enviar heartbeat manualmente
await fetch('/api/invoice-points/session/heartbeat', {
  method: 'POST',
  headers: { 'Authorization': 'Bearer YOUR_TOKEN' }
});

// 3. Esperar 11+ minutos sin heartbeat y verificar que se liberó
```

### Test de Liberación Automática

1. Asignar un punto de emisión
2. NO enviar heartbeat
3. Esperar 10-13 minutos
4. Verificar en logs del servidor que se liberó
5. Intentar usar el punto - debe estar disponible

---

## 📞 Soporte

Para dudas o problemas:
- Backend: Revisar logs del servicio `SessionCleanupService`
- Frontend: Verificar que heartbeat se esté enviando correctamente
- Base de datos: Consultar tabla `user_invoice_point_session`

```sql
-- Ver sesiones activas y su última actividad
SELECT 
  u.user_name,
  s.id_invoice_issuance_point,
  s.start_time,
  s.last_activity,
  EXTRACT(EPOCH FROM (NOW() - s.last_activity))/60 as minutes_inactive,
  s.active
FROM user_invoice_point_session s
JOIN users u ON s.id_user = u.id_user
WHERE s.active = true
ORDER BY s.last_activity DESC;
```

---

**Última actualización:** Octubre 2025  
**Versión:** 1.0.0

