# 🔒 Especificación: Validación de Sesión - ebill-authentication

## 📋 **CONTEXTO**

El endpoint `GET /api/session/validate` actualmente falla con **Error 500** cuando un usuario tiene múltiples sesiones activas (múltiples puntos de emisión asignados simultáneamente).

**Servicio afectado**: **ebill-authentication** (puerto 8280)

---

## ⚠️ **PROBLEMA ACTUAL**

### **Escenario:**

1. Usuario hace login en **Punto A** (no cierra sesión)
2. Usuario hace login en **Punto B** (sesión anterior queda activa)
3. Usuario navega a cualquier página protegida
4. Frontend llama a `/api/session/validate`
5. **Backend retorna Error 500** (no sabe cuál sesión retornar)

### **Resultado:**

- ❌ Frontend recibe 500 → fuerza logout
- ❌ Usuario no puede trabajar
- ❌ Pérdida de productividad

---

## 🎯 **SOLUCIÓN PROPUESTA**

### **OPCIÓN 1: Invalidar Sesiones Previas en Login** ⭐ (RECOMENDADO)

**Regla de negocio:**  
✅ Un usuario solo puede estar en **UN punto de emisión a la vez**.

**Implementación en Login:**

```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // 1. Validar credenciales
    User user = userService.authenticate(request.getUsername(), request.getPassword());

    if (user == null) {
        return ResponseEntity.status(401).body("Credenciales inválidas");
    }

    // 2. ⭐ INVALIDAR TODAS LAS SESIONES ACTIVAS DEL USUARIO
    sessionRepository.invalidateAllByUsername(user.getUsername());
    logger.info("✅ Sesiones previas invalidadas para usuario: {}", user.getUsername());

    // 3. Crear NUEVA sesión (única)
    Session newSession = createNewSession(user, request);
    sessionRepository.save(newSession);

    // 4. Generar token JWT con el sessionId
    String token = jwtService.generateToken(user, newSession.getId());

    return ResponseEntity.ok(new LoginResponse(token, user));
}
```

**Query en Repository:**

```java
@Modifying
@Transactional
@Query("UPDATE Session s SET s.isActive = false, s.invalidatedAt = CURRENT_TIMESTAMP " +
       "WHERE s.username = :username AND s.isActive = true")
int invalidateAllByUsername(@Param("username") String username);
```

---

### **OPCIÓN 2: Retornar Lista + Frontend Elige** (Si no se puede invalidar)

Si **NO** se puede invalidar las sesiones previas en login (por restricciones de negocio), el backend debería:

#### **Comportamiento esperado:**

1. ✅ Si hay **1 sola sesión activa** → retornar **objeto único**
2. ✅ Si hay **múltiples sesiones activas** → retornar **array ordenado por fecha desc**

#### **Response cuando hay 1 sesión:**

```json
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
```

#### **Response cuando hay múltiples sesiones:**

```json
[
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
      "idInvoicePoint": 2,
      "code": "002"
    },
    "lastActivity": "2025-12-13T16:30:00Z"
  },
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
    },
    "lastActivity": "2025-12-13T15:45:00Z"
  }
]
```

**Notas:**

- ✅ Ordenar por `lastActivity DESC` (más reciente primero)
- ✅ Frontend tomará la **primera sesión** del array
- ✅ Frontend logeará warning en consola para debugging

**Implementación Backend:**

```java
@GetMapping("/validate")
public ResponseEntity<?> validateSession(HttpServletRequest request) {
    String token = extractToken(request);

    // 1. Validar token JWT
    Claims claims = jwtService.validateToken(token);
    String username = claims.getSubject();

    // 2. Buscar sesiones activas
    List<Session> activeSessions = sessionRepository.findActiveByUsername(username);

    if (activeSessions.isEmpty()) {
        return ResponseEntity.status(401).json(Map.of(
            "valid", false,
            "reason", "session_not_found"
        ));
    }

    // 3. Si hay 1 sola sesión, retornar objeto único
    if (activeSessions.size() == 1) {
        return ResponseEntity.ok(buildResponse(activeSessions.get(0)));
    }

    // 4. Si hay múltiples, retornar lista ordenada
    logger.warn("⚠️ Usuario {} tiene {} sesiones activas", username, activeSessions.size());

    List<SessionValidationResponse> responses = activeSessions.stream()
        .sorted(Comparator.comparing(Session::getLastActivity).reversed())
        .map(this::buildResponse)
        .collect(Collectors.toList());

    return ResponseEntity.ok(responses);
}
```

---

## 🔧 **ESTADO ACTUAL DEL FRONTEND**

✅ **Ya implementado**:

- ✅ Manejo de **objeto único** (caso normal)
- ✅ Manejo de **array** (múltiples sesiones)
- ✅ Selección automática de la **sesión más reciente**
- ✅ Logs de warning cuando detecta múltiples sesiones
- ✅ Restauración de datos en localStorage

**Código implementado:**

```typescript
.get<SessionValidationResponse | SessionValidationResponse[]>(
  `${this.baseUrl}/validate`
)
.pipe(
  map((response) => {
    // CASO 1: Backend retorna una lista (múltiples sesiones)
    if (Array.isArray(response)) {
      this.logger.warn(
        `[SessionValidation] ⚠️ Múltiples sesiones detectadas: ${response.length}`
      );

      const validSessions = response.filter((s) => s.valid);

      if (validSessions.length === 0) {
        return { valid: false, reason: 'session_not_found' };
      }

      // Usar la primera sesión (más reciente)
      const selectedSession = validSessions[0];
      this.logger.warn(
        `[SessionValidation] 🎯 Usando sesión: Punto ${selectedSession.invoicePoint?.code}`
      );

      return selectedSession;
    }

    // CASO 2: Backend retorna un objeto único (normal)
    return response;
  })
)
```

---

## 📊 **COMPARACIÓN DE SOLUCIONES**

| Característica          | Opción 1: Invalidar en Login | Opción 2: Retornar Lista |
| ----------------------- | ---------------------------- | ------------------------ |
| **Simplicidad**         | ⭐⭐⭐⭐⭐                   | ⭐⭐⭐                   |
| **Rendimiento**         | ⭐⭐⭐⭐⭐                   | ⭐⭐⭐⭐                 |
| **Seguridad**           | ⭐⭐⭐⭐⭐                   | ⭐⭐⭐                   |
| **Experiencia Usuario** | ⭐⭐⭐⭐⭐                   | ⭐⭐⭐                   |
| **Evita Error 500**     | ✅ Sí                        | ✅ Sí                    |
| **Previene Conflictos** | ✅ Sí                        | ⚠️ Parcial               |
| **Facilidad Backend**   | ⭐⭐⭐⭐                     | ⭐⭐                     |

---

## 🎯 **RECOMENDACIÓN FINAL**

### **Implementar OPCIÓN 1** ⭐

**Razones:**

1. ✅ **Más simple**: Una sola línea de código en login
2. ✅ **Más seguro**: Usuario solo en un punto a la vez
3. ✅ **Mejor UX**: No hay sorpresas de "estás en otro punto"
4. ✅ **Evita problemas futuros**: Factura en Punto A, NC en Punto B (inconsistencias)
5. ✅ **Alineado con regla de negocio**: Un usuario = un punto de emisión

**Si Opción 1 no es viable temporalmente:**

- Implementar **Opción 2** como solución temporal
- Frontend **ya está preparado** para manejar ambos casos
- Migrar a Opción 1 cuando sea posible

---

## 📋 **CHECKLIST DE VALIDACIÓN**

### **Para Opción 1:**

- [ ] Implementar `invalidateAllByUsername()` en SessionRepository
- [ ] Llamar a `invalidateAllByUsername()` en el método `login()`
- [ ] Testing: Login 2 veces con mismo usuario
- [ ] Verificar en BD que solo hay 1 sesión activa
- [ ] Testing en frontend: `/welcome` funciona sin Error 500

### **Para Opción 2:**

- [ ] Modificar `/validate` para retornar array si `activeSessions.size() > 1`
- [ ] Ordenar por `lastActivity DESC`
- [ ] Testing: Usuario con 2 sesiones activas
- [ ] Verificar que frontend selecciona la sesión correcta
- [ ] Logs en backend cuando detecta múltiples sesiones

---

## 🚀 **TESTING**

### **Caso de prueba:**

```bash
# 1. Login en Punto 001
curl -X POST http://localhost:8280/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test",
    "password": "test123",
    "invoicePointId": 1
  }'

# Guardar token1
TOKEN1="eyJhbGc..."

# 2. Login NUEVAMENTE en Punto 002
curl -X POST http://localhost:8280/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test",
    "password": "test123",
    "invoicePointId": 2
  }'

# Guardar token2
TOKEN2="eyJhbGc..."

# 3. Validar sesión con TOKEN1 (debería ser inválida si Opción 1 implementada)
curl http://localhost:8280/api/session/validate \
  -H "Authorization: Bearer $TOKEN1"

# Expected (Opción 1): {"valid": false, "reason": "session_not_found"}

# 4. Validar sesión con TOKEN2 (debería ser válida)
curl http://localhost:8280/api/session/validate \
  -H "Authorization: Bearer $TOKEN2"

# Expected (Opción 1): {objeto único con invoicePointId: 2}

# 5. Verificar en BD
SELECT username, invoice_point_id, is_active, created_at, last_activity
FROM session
WHERE username = 'test'
ORDER BY created_at DESC;

# Expected (Opción 1): Solo 1 registro con is_active = true
```

---

## 📞 **CONTACTO**

- **Equipo Frontend**: Ya implementado y listo para probar
- **Equipo Backend (ebill-authentication)**: Necesita implementar una de las 2 opciones
- **Prioridad**: 🔴 Alta (bloquea funcionalidad de Welcome Dashboard)

---

**Fecha**: 13 de Diciembre, 2025  
**Desarrollado por**: Frontend Team  
**Target**: Backend Team (ebill-authentication)  
**Versión**: 1.0
