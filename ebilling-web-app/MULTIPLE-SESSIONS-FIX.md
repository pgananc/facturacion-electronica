# 🔒 Solución al Problema de Sesiones Múltiples

## ⚠️ **IMPORTANTE: DOCUMENTO DE REFERENCIA PARA BACKEND**

**Este documento es una guía para el equipo de backend (ebill-authentication).**  
**NO se han desactivado validaciones de seguridad en el frontend.**

---

## ⚠️ **PROBLEMA IDENTIFICADO**

**Síntoma**: Error 500 en `/api/session/validate` cuando un usuario tiene múltiples sesiones activas.

**Servicio afectado**: **ebill-authentication** (puerto 8280)

**Causa**: El endpoint de **login** crea una nueva sesión pero **NO invalida** las sesiones anteriores del mismo usuario.

**Resultado**: Usuario con 2+ sesiones activas → validación falla → error 500 → logout forzado.

---

## 🎯 **SOLUCIONES PROPUESTAS**

### **SOLUCIÓN 1: Invalidar Sesiones Previas en Login** ⭐ (RECOMENDADO)

#### **Backend: ebill-authentication**

**Ubicación**: `AuthController.java` o `AuthenticationService.java`

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
    // O si no existe el método:
    // sessionRepository.deleteByUsernameAndIsActive(user.getUsername(), true);

    // 3. Crear NUEVA sesión (única)
    Session newSession = new Session();
    newSession.setUsername(user.getUsername());
    newSession.setIpAddress(getClientIP(request));
    newSession.setBrowserInfo(getBrowserInfo(request));
    newSession.setIsActive(true);
    newSession.setCreatedAt(LocalDateTime.now());
    newSession.setLastActivity(LocalDateTime.now());

    sessionRepository.save(newSession);

    // 4. Generar token JWT con el sessionId
    String token = jwtService.generateToken(user, newSession.getId());

    return ResponseEntity.ok(new LoginResponse(token, user));
}
```

**Método en Repository**:

```java
public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * Invalida todas las sesiones activas de un usuario
     */
    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.username = :username AND s.isActive = true")
    void invalidateAllByUsername(@Param("username") String username);

    /**
     * Cuenta las sesiones activas de un usuario
     */
    @Query("SELECT COUNT(s) FROM Session s WHERE s.username = :username AND s.isActive = true")
    long countActiveSessionsByUsername(@Param("username") String username);
}
```

---

### **SOLUCIÓN 2: Constraint de Base de Datos (1 Sesión por Usuario)**

#### **Agregar Constraint UNIQUE**

```sql
-- PostgreSQL / MySQL
ALTER TABLE session
DROP CONSTRAINT IF EXISTS unique_active_session_per_user;

ALTER TABLE session
ADD CONSTRAINT unique_active_session_per_user
UNIQUE (username, is_active)
WHERE is_active = true;
```

**Nota**: Esto **fuerza** a que solo pueda existir 1 sesión activa por usuario.

**Ventaja**: Garantiza integridad a nivel de BD.  
**Desventaja**: Si quieres permitir múltiples sesiones (ej: móvil + desktop), no funciona.

---

### **SOLUCIÓN 3: Limpieza de Sesiones Antiguas (Scheduled Task)**

#### **Backend: Tarea Programada**

```java
@Component
public class SessionCleanupTask {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private LoggerService logger;

    /**
     * Limpia sesiones inactivas cada 15 minutos
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // 15 minutos
    public void cleanupInactiveSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);

        int deleted = sessionRepository.deleteByLastActivityBefore(threshold);

        if (deleted > 0) {
            logger.info("🧹 Sesiones inactivas limpiadas: " + deleted);
        }
    }

    /**
     * Limpia sesiones antiguas cada día
     */
    @Scheduled(cron = "0 0 2 * * *") // 2 AM todos los días
    public void cleanupOldSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        int deleted = sessionRepository.deleteByCreatedAtBefore(threshold);

        logger.info("🧹 Sesiones antiguas limpiadas (>7 días): " + deleted);
    }
}
```

**Método en Repository**:

```java
public interface SessionRepository extends JpaRepository<Session, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Session s WHERE s.lastActivity < :threshold")
    int deleteByLastActivityBefore(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Transactional
    @Query("DELETE FROM Session s WHERE s.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}
```

**Habilitar Scheduling**:

```java
@SpringBootApplication
@EnableScheduling // ⭐ Agregar esta anotación
public class EbillAuthenticationApplication {
    public static void main(String[] args) {
        SpringApplication.run(EbillAuthenticationApplication.class, args);
    }
}
```

---

### **SOLUCIÓN 4: Mejorar Validación de Sesión (Handle Multiple Sessions)**

#### **Backend: SessionValidationService**

En lugar de fallar con error 500, manejar graciosamente:

```java
@Service
public class SessionValidationService {

    public SessionValidationResponse validateSession(String token, String ipAddress, String browser) {
        // 1. Decodificar token
        Claims claims = jwtService.validateToken(token);
        String username = claims.getSubject();
        Long sessionId = claims.get("sessionId", Long.class);

        // 2. Buscar sesión en BD
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {
            return SessionValidationResponse.invalid("Sesión no encontrada");
        }

        Session session = sessionOpt.get();

        // 3. Verificar si hay múltiples sesiones activas
        long activeSessionsCount = sessionRepository.countActiveSessionsByUsername(username);

        if (activeSessionsCount > 1) {
            // ⭐ OPCIÓN A: Invalidar las otras sesiones y mantener esta
            sessionRepository.invalidateAllExcept(username, sessionId);
            logger.warn("Usuario {} tenía {} sesiones activas. Se invalidaron las antiguas.",
                username, activeSessionsCount);

            // ⭐ OPCIÓN B: Retornar advertencia pero permitir acceso
            // return SessionValidationResponse.warning("Múltiples sesiones detectadas");
        }

        // 4. Validar IP y browser (opcional, puede ser muy estricto)
        if (!session.getIpAddress().equals(ipAddress)) {
            logger.warn("IP cambió para usuario {}: {} -> {}",
                username, session.getIpAddress(), ipAddress);
            // Decidir si invalidar o solo advertir
        }

        // 5. Actualizar última actividad
        session.setLastActivity(LocalDateTime.now());
        sessionRepository.save(session);

        return SessionValidationResponse.valid();
    }
}
```

**Método en Repository**:

```java
@Modifying
@Query("UPDATE Session s SET s.isActive = false " +
       "WHERE s.username = :username AND s.id != :currentSessionId AND s.isActive = true")
void invalidateAllExcept(@Param("username") String username,
                         @Param("currentSessionId") Long currentSessionId);
```

---

## 🎯 **RECOMENDACIÓN FINAL**

**Implementar SOLUCIÓN 1 + SOLUCIÓN 3**:

1. ✅ **En Login**: Invalidar sesiones previas (evita el problema desde el origen)
2. ✅ **Scheduled Task**: Limpiar sesiones antiguas (mantiene BD limpia)

**Opcional**: 3. ⚠️ **Mejorar Validación**: En lugar de error 500, manejar múltiples sesiones graciosamente

---

## 🚀 **IMPLEMENTACIÓN PASO A PASO**

### **Paso 1: Actualizar AuthController**

```java
// En el método login(), después de validar credenciales:
sessionRepository.invalidateAllByUsername(user.getUsername());
```

### **Paso 2: Agregar Método en Repository**

```java
@Modifying
@Query("UPDATE Session s SET s.isActive = false WHERE s.username = :username")
void invalidateAllByUsername(@Param("username") String username);
```

### **Paso 3: (Opcional) Agregar Scheduled Task**

```java
@Component
public class SessionCleanupTask {
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void cleanupInactiveSessions() {
        // Implementación arriba
    }
}
```

### **Paso 4: Testing**

```bash
# 1. Login con usuario A
curl -X POST http://localhost:8280/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'

# 2. Login NUEVAMENTE con usuario A (desde otra IP/browser simulado)
curl -X POST http://localhost:8280/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'

# 3. Verificar en BD que solo hay 1 sesión activa
SELECT * FROM session WHERE username = 'test' AND is_active = true;
-- Debería retornar SOLO 1 registro
```

---

## 📋 **CHECKLIST DE VALIDACIÓN**

- [ ] Implementado `invalidateAllByUsername()` en Repository
- [ ] Actualizado método `login()` para llamar a `invalidateAllByUsername()`
- [ ] Testing manual: login 2 veces con mismo usuario
- [ ] Verificar en BD que solo queda 1 sesión activa
- [ ] Testing en frontend: `/welcome` ya no da error 500
- [ ] (Opcional) Scheduled task implementado
- [ ] (Opcional) Logs de sesiones múltiples detectadas

---

## ⚠️ **NOTA IMPORTANTE**

**NO se ha desactivado ninguna validación de seguridad en el frontend.**

Este documento es solo una **referencia técnica** para que el equipo de backend implemente la solución correcta en **ebill-authentication** (puerto 8280).

---

**Fecha**: 13 de Diciembre, 2025  
**Desarrollado por**: Frontend Team (documentación de requerimientos)  
**Target**: Backend Team (ebill-authentication)  
**Versión**: 1.0
