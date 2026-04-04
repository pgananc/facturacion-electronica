# 🔐 Requerimientos: Manejo de Sesión con Fingerprinting

## 📋 Resumen Ejecutivo

Implementar un sistema de validación de sesión basado en **IP + Browser Fingerprint** que permita:

- Restaurar sesión automáticamente si el usuario cierra el browser accidentalmente (mismo browser + misma IP)
- Forzar re-autenticación si el usuario cambia de browser, IP o dispositivo
- Mantener la seguridad del sistema de facturación electrónica

---

## 🎯 Reglas de Negocio

| Condición                                                | Acción                        |
| -------------------------------------------------------- | ----------------------------- |
| Misma IP + Mismo browser + Token válido + Punto asignado | ✅ Restaurar sesión → Welcome |
| Diferente IP                                             | 🔐 Forzar login               |
| Diferente browser                                        | 🔐 Forzar login               |
| Token expirado (JWT)                                     | 🔐 Forzar login               |
| Punto de emisión liberado (timeout heartbeat)            | 🔐 Forzar login               |
| VPN detectada (cambio de IP)                             | 🔐 Forzar login               |

---

## 🔄 Flujos de Usuario

### Flujo 1: Usuario cierra browser accidentalmente (mismo equipo)

```
1. Usuario trabajando normalmente
2. Cierra browser sin hacer logout (accidente)
3. Heartbeat deja de enviar señales
4. [Si regresa ANTES del timeout del heartbeat]
   → Abre mismo browser
   → Frontend detecta token en localStorage
   → Llama a /api/session/validate
   → Backend verifica: IP ✅ + Browser ✅ + Token ✅ + Punto asignado ✅
   → Restaura sesión → Navega a /welcome con punto de emisión activo
```

### Flujo 2: Usuario cierra browser y regresa DESPUÉS del timeout

```
1. Usuario cierra browser
2. Heartbeat deja de enviar señales
3. [Pasa el tiempo de timeout - ej: 5 minutos]
4. Backend libera el punto de emisión automáticamente
5. Usuario regresa, abre mismo browser
6. Frontend detecta token en localStorage
7. Llama a /api/session/validate
8. Backend verifica: IP ✅ + Browser ✅ + Token ✅ + Punto asignado ❌
9. Responde: { valid: false, reason: "invoice_point_released" }
10. Frontend limpia storage → Navega a /login
11. Usuario hace login → Selecciona compañía → Selecciona punto de emisión
```

### Flujo 3: Usuario intenta usar sesión desde otro browser

```
1. Usuario tiene sesión activa en Chrome
2. Abre Firefox e ingresa la URL
3. Firefox no tiene token en localStorage → Navega a /login
4. (O si copiara el token manualmente):
   → Frontend llama a /api/session/validate
   → Backend verifica: IP ✅ + Browser ❌ (Firefox ≠ Chrome)
   → Responde: { valid: false, reason: "browser_mismatch" }
   → Frontend limpia storage → Navega a /login
```

### Flujo 4: Usuario intenta usar sesión desde otra IP (VPN, otra red)

```
1. Usuario tiene sesión activa en oficina (IP: 190.x.x.x)
2. Va a casa y abre el mismo browser (IP: 181.x.x.x)
3. Frontend detecta token en localStorage
4. Llama a /api/session/validate
5. Backend verifica: IP ❌ (181 ≠ 190)
6. Responde: { valid: false, reason: "ip_mismatch" }
7. Frontend limpia storage → Navega a /login
```

---

## 🏗️ Arquitectura de la Solución

### Componentes Involucrados

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ localStorage│  │ AppComponent│  │ SessionValidationService│  │
│  │ (token,     │  │ (init)      │  │ (validateSession)       │  │
│  │  fingerprint│  │             │  │                         │  │
│  │  )          │  │             │  │                         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         BACKEND                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ ebill-auth  │  │ ebill-billing│ │ Base de Datos           │  │
│  │ (login,     │  │ (invoice    │  │ (session_fingerprint    │  │
│  │  validate)  │  │  points)    │  │  table)                 │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📦 Responsabilidades

### 🔵 BACKEND (ebill-auth)

#### 1. Modificar endpoint de LOGIN

**Endpoint:** `POST /api/login`

**Responsabilidad:** Al autenticar exitosamente, guardar fingerprint de sesión.

**Cambios requeridos:**

```java
// Request (ya existente)
{
  "userName": "1722633961",
  "password": "xxx"
}

// Lógica adicional al procesar login:
1. Extraer IP del request (X-Forwarded-For o RemoteAddr)
2. Extraer User-Agent del header
3. Generar sessionId único (UUID)
4. Guardar en BD:
   - sessionId
   - userId
   - ipAddress
   - userAgent (o parsear solo browser base: "Chrome", "Firefox", etc.)
   - createdAt
   - lastActivityAt

// Response (agregar sessionId)
{
  "token": "eyJhbGc...",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"  // NUEVO
}
```

#### 2. Nuevo endpoint de VALIDACIÓN DE SESIÓN

**Endpoint:** `GET /api/session/validate`

**Headers requeridos:**

- `Authorization: Bearer <token>`

**Responsabilidad:** Validar que la sesión actual coincide con el fingerprint guardado.

**Lógica:**

```java
1. Extraer token del header Authorization
2. Decodificar JWT → obtener userId
3. Extraer IP actual del request
4. Extraer User-Agent actual del request
5. Buscar en BD el fingerprint guardado para ese userId
6. Comparar:
   - ¿IP actual == IP guardada?
   - ¿Browser actual == Browser guardado? (comparar base, no versión exacta)
   - ¿Token no expirado?
7. Consultar ebill-billing: ¿El usuario tiene punto de emisión asignado?

// Response exitoso
{
  "valid": true,
  "user": {
    "userName": "1722633961",
    "name": "Jonathan Guerrero",
    "roles": ["super_admin"]
  },
  "company": {
    "idCompany": 1,
    "name": "POWER OUT",
    "identification": "1722633961001"
  },
  "establishment": {
    "idEstablishment": 1,
    "name": "Matriz",
    "code": "001"
  },
  "invoicePoint": {
    "idInvoicePoint": 1,
    "code": "001"
  }
}

// Response fallido
{
  "valid": false,
  "reason": "ip_mismatch" | "browser_mismatch" | "token_expired" | "invoice_point_released" | "session_not_found"
}
```

#### 3. Modificar endpoint de LOGOUT

**Endpoint:** `DELETE /api/login/logout`

**Responsabilidad adicional:** Eliminar el fingerprint de sesión de la BD.

```java
// Lógica adicional:
1. Extraer userId del token
2. DELETE FROM session_fingerprint WHERE user_id = :userId
```

#### 4. Nueva tabla en Base de Datos

**Tabla:** `session_fingerprint`

```sql
CREATE TABLE session_fingerprint (
    id BIGSERIAL PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    ip_address VARCHAR(45) NOT NULL,        -- IPv4 o IPv6
    user_agent VARCHAR(500) NOT NULL,       -- User-Agent completo
    browser_base VARCHAR(50) NOT NULL,      -- "Chrome", "Firefox", "Safari", etc.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_session_user ON session_fingerprint(user_id);
CREATE INDEX idx_session_id ON session_fingerprint(session_id);
```

---

### 🟢 FRONTEND (ebilling-web-app)

#### 1. Cambiar de sessionStorage a localStorage

**Archivos afectados:**

- `src/app/app.module.ts` (tokenGetter)
- `src/app/pages/login/login.component.ts`
- `src/app/pages/welcome/welcome.component.ts`
- `src/app/_service/auth/login.service.ts`
- Cualquier archivo que use `sessionStorage.getItem(environment.TOKEN)`

**Cambio:**

```typescript
// ANTES
sessionStorage.setItem(environment.TOKEN, token);
sessionStorage.getItem(environment.TOKEN);

// DESPUÉS
localStorage.setItem(environment.TOKEN, token);
localStorage.getItem(environment.TOKEN);
```

#### 2. Nuevo servicio: SessionValidationService

**Archivo:** `src/app/_service/auth/session-validation.service.ts`

**Responsabilidad:** Llamar al backend para validar la sesión al cargar la app.

```typescript
@Injectable({ providedIn: "root" })
export class SessionValidationService {
  constructor(private http: HttpClient) {}

  /**
   * Valida la sesión actual contra el backend
   * Verifica: IP + Browser + Token + Punto de emisión
   */
  validateSession(): Observable<SessionValidationResponse> {
    return this.http.get<SessionValidationResponse>(`${environment.HOST_AUTH}/api/session/validate`);
  }
}

interface SessionValidationResponse {
  valid: boolean;
  reason?: "ip_mismatch" | "browser_mismatch" | "token_expired" | "invoice_point_released" | "session_not_found";
  user?: { userName: string; name: string; roles: string[] };
  company?: { idCompany: number; name: string; identification: string };
  establishment?: { idEstablishment: number; name: string; code: string };
  invoicePoint?: { idInvoicePoint: number; code: string };
}
```

#### 3. Modificar AppComponent o crear Guard

**Opción A: En AppComponent.ngOnInit()**

```typescript
ngOnInit() {
  // Si hay token en localStorage, validar sesión
  const token = localStorage.getItem(environment.TOKEN);

  if (token && !this.isPublicRoute()) {
    this.sessionValidationService.validateSession().subscribe({
      next: (response) => {
        if (response.valid) {
          // Restaurar datos en memoria/storage
          this.restoreSession(response);
          // Navegar a welcome o página anterior
          this.router.navigate(['/welcome']);
        } else {
          // Sesión inválida - limpiar y ir a login
          this.clearSessionAndRedirect(response.reason);
        }
      },
      error: (err) => {
        // Error de red o servidor - limpiar y ir a login
        this.clearSessionAndRedirect('network_error');
      }
    });
  }
}
```

**Opción B: AuthGuard mejorado**

```typescript
@Injectable({ providedIn: "root" })
export class SessionValidationGuard implements CanActivate {
  canActivate(): Observable<boolean> {
    const token = localStorage.getItem(environment.TOKEN);

    if (!token) {
      this.router.navigate(["/login"]);
      return of(false);
    }

    return this.sessionValidationService.validateSession().pipe(
      map((response) => {
        if (response.valid) {
          this.restoreSession(response);
          return true;
        } else {
          this.clearSession();
          this.router.navigate(["/login"]);
          return false;
        }
      }),
      catchError(() => {
        this.clearSession();
        this.router.navigate(["/login"]);
        return of(false);
      })
    );
  }
}
```

#### 4. Guardar sessionId del login

**Archivo:** `src/app/pages/login/login.component.ts`

```typescript
// Al recibir respuesta exitosa del login:
localStorage.setItem(environment.TOKEN, response.token);
localStorage.setItem("SESSION_ID", response.sessionId); // NUEVO
```

#### 5. Limpiar localStorage en logout

**Archivo:** `src/app/_service/auth/login.service.ts`

```typescript
closeSession() {
  // Llamar al backend para liberar punto de emisión y eliminar fingerprint
  this.http.delete(`${this.url}/logout`).subscribe({
    next: () => {
      localStorage.clear();  // Limpiar TODO
      this.router.navigate(['/login']);
    },
    error: () => {
      localStorage.clear();
      this.router.navigate(['/login']);
    }
  });
}
```

---

## 🔐 Consideraciones de Seguridad

### 1. Extracción de IP en Backend

```java
// El backend debe extraer la IP correctamente considerando proxies
String clientIp = request.getHeader("X-Forwarded-For");
if (clientIp == null || clientIp.isEmpty()) {
    clientIp = request.getHeader("X-Real-IP");
}
if (clientIp == null || clientIp.isEmpty()) {
    clientIp = request.getRemoteAddr();
}
// Si X-Forwarded-For tiene múltiples IPs, tomar la primera (cliente original)
if (clientIp != null && clientIp.contains(",")) {
    clientIp = clientIp.split(",")[0].trim();
}
```

### 2. Parsing de User-Agent

```java
// Extraer solo el browser base para comparación flexible
// Ejemplo: "Mozilla/5.0 ... Chrome/131.0.0.0 Safari/537.36"

public String extractBrowserBase(String userAgent) {
    if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) {
        return "Chrome";
    } else if (userAgent.contains("Firefox")) {
        return "Firefox";
    } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
        return "Safari";
    } else if (userAgent.contains("Edg")) {
        return "Edge";
    } else if (userAgent.contains("Opera") || userAgent.contains("OPR")) {
        return "Opera";
    }
    return "Unknown";
}
```

### 3. Protección contra Session Fixation

- Generar nuevo `sessionId` en cada login exitoso
- No reutilizar sessionId de sesiones anteriores

### 4. Limpieza de sesiones antiguas

```sql
-- Job programado para limpiar sesiones inactivas (ej: más de 24 horas)
DELETE FROM session_fingerprint
WHERE last_activity_at < NOW() - INTERVAL '24 hours';
```

---

## 📊 Matriz de Responsabilidades (RACI)

| Tarea                                 | Backend | Frontend | DevOps |
| ------------------------------------- | ------- | -------- | ------ |
| Crear tabla session_fingerprint       | **R**   | I        | I      |
| Modificar POST /api/login             | **R**   | I        | -      |
| Crear GET /api/session/validate       | **R**   | I        | -      |
| Modificar DELETE /api/login/logout    | **R**   | I        | -      |
| Cambiar sessionStorage → localStorage | I       | **R**    | -      |
| Crear SessionValidationService        | I       | **R**    | -      |
| Modificar AppComponent/Guard          | I       | **R**    | -      |
| Guardar sessionId en login            | I       | **R**    | -      |
| Configurar nginx para pasar headers   | I       | I        | **R**  |

**R** = Responsable (hace el trabajo)  
**I** = Informado

---

## 🧪 Casos de Prueba

### Caso 1: Login normal y validación exitosa

1. Usuario hace login desde Chrome en IP 190.x.x.x
2. Cierra browser
3. Abre Chrome (mismo) en misma red (IP 190.x.x.x)
4. **Esperado:** Sesión restaurada automáticamente

### Caso 2: Cambio de browser

1. Usuario hace login desde Chrome
2. Intenta acceder desde Firefox (misma IP)
3. **Esperado:** Redirigido a login

### Caso 3: Cambio de IP (VPN)

1. Usuario hace login desde IP 190.x.x.x
2. Conecta VPN (IP cambia a 85.x.x.x)
3. Intenta acceder
4. **Esperado:** Redirigido a login

### Caso 4: Timeout de heartbeat

1. Usuario hace login y selecciona punto de emisión
2. Cierra browser
3. Espera más del timeout (ej: 5 minutos)
4. Abre mismo browser, misma IP
5. **Esperado:** Redirigido a login (punto liberado)

### Caso 5: Múltiples pestañas

1. Usuario tiene 2 pestañas abiertas
2. Cierra una pestaña
3. **Esperado:** Sesión sigue activa en la otra pestaña

---

## 📅 Estimación de Esfuerzo

| Componente                          | Esfuerzo Estimado |
| ----------------------------------- | ----------------- |
| Backend - Tabla y entidad           | 2 horas           |
| Backend - Modificar login           | 3 horas           |
| Backend - Endpoint validate         | 4 horas           |
| Backend - Modificar logout          | 1 hora            |
| Backend - Tests                     | 3 horas           |
| Frontend - Cambiar storage          | 2 horas           |
| Frontend - SessionValidationService | 2 horas           |
| Frontend - Modificar init/guard     | 3 horas           |
| Frontend - Tests                    | 2 horas           |
| **Total**                           | **~22 horas**     |

---

## 📝 Notas Adicionales

1. **Migración:** Al desplegar, los usuarios con sesión activa (en sessionStorage) perderán la sesión y deberán hacer login nuevamente. Esto es esperado y solo ocurre una vez.

2. **Compatibilidad:** Esta funcionalidad es independiente del heartbeat existente. Ambos sistemas trabajan juntos:

   - Heartbeat: Mantiene el punto de emisión asignado
   - Fingerprint: Valida que la sesión sea del mismo usuario/dispositivo

3. **Logs:** Implementar logging en el backend para auditoría de intentos de validación fallidos (posibles intentos de robo de sesión).

---

**Fecha de creación:** Diciembre 2024  
**Versión:** 1.1  
**Estado:** ✅ Implementado

---

## 📋 Historial de Implementación

### Frontend (Diciembre 2024) ✅ COMPLETADO

**Archivos creados:**

- `src/app/_service/auth/session-validation.service.ts` - Servicio de validación con caché
- `src/app/_service/canActivate/session-validation.guard.ts` - Guard para rutas protegidas

**Archivos modificados:**

- `src/app/app-routing.module.ts` - Agregado SessionValidationGuard a rutas protegidas
- 30 archivos: Cambiado `sessionStorage` → `localStorage`

**Rutas protegidas con SessionValidationGuard:**

- `/welcome`
- `/profile`
- `/clients`
- `/products`
- `/users`
- `/companies`
- `/invoices`
- `/establishments`
- `/invoice-points`
- `/company-users`
- `/admin`
- `/setting`

**Rutas públicas (sin guard):**

- `/login`
- `/reset-password`
- `/invoice-consultation/*`
- `/not-403`
- `/not-404`

### Backend (Diciembre 2024) ✅ COMPLETADO

- Endpoint `GET /api/session/validate` implementado
- Tabla `session_fingerprint` creada
- Validación de IP + User-Agent implementada
