# 🚦 Propuesta de Implementación de Rate Limiting

**Fecha:** 2025-01-XX  
**Estado:** 📋 Propuesta (No implementado)

---

## 📋 Resumen Ejecutivo

Esta propuesta detalla la implementación de rate limiting para tres endpoints críticos del sistema de facturación electrónica, con el objetivo de prevenir abuso de consultas y descargas masivas.

### Endpoints a Proteger

| Endpoint | Método | Razón | Límites Requeridos |
|----------|--------|-------|-------------------|
| `/api/invoice/documents/query` | POST | Abuso de consultas | 100 req/min por token<br>500 req/hora por IP |
| `/api/invoice/{id}/pdf` | GET | Abuso de descargas | 50 descargas/hora por token<br>200 descargas/hora por IP |
| `/api/invoice/{id}/xml` | GET | Abuso de descargas | 50 descargas/hora por token<br>200 descargas/hora por IP |

---

## 🎯 Objetivos

1. **Prevenir abuso de consultas** en el endpoint de búsqueda de documentos
2. **Limitar descargas masivas** de archivos PDF y XML
3. **Configuración dinámica** mediante tabla `params` en base de datos
4. **Tracking por token JWT y por IP** para doble protección
5. **Respuestas HTTP estándar** (429 Too Many Requests) cuando se excedan los límites

---

## 🏗️ Arquitectura Propuesta

### Componentes a Crear

```
src/main/java/com/wallparisoft/ebillbilling/
├── config/
│   └── RateLimitingConfig.java          # Configuración de Redis para rate limiting
├── service/
│   ├── RateLimitingService.java          # Interfaz del servicio
│   └── impl/
│       └── RateLimitingServiceImpl.java  # Implementación con Redis
├── annotation/
│   └── RateLimited.java                  # Anotación para marcar endpoints
├── aspect/
│   └── RateLimitingAspect.java           # Aspecto AOP para interceptar llamadas
└── exception/
    └── RateLimitExceededException.java   # Excepción personalizada
```

### Flujo de Funcionamiento

```
1. Cliente hace request → 
2. RateLimitingAspect intercepta → 
3. RateLimitingService verifica límites (Redis) → 
4. Si excede → Lanza RateLimitExceededException → 
5. GlobalExceptionHandler retorna HTTP 429 → 
6. Si no excede → Continúa al controller
```

---

## 📊 Configuración en Base de Datos

### Parámetros a Crear en Tabla `params`

| code_param | value_param | description_param |
|------------|-------------|------------------|
| `RATE_LIMIT_QUERY_REQ_PER_MIN_TOKEN` | `100` | Límite de requests por minuto por token para `/api/invoice/documents/query` |
| `RATE_LIMIT_QUERY_REQ_PER_HOUR_IP` | `500` | Límite de requests por hora por IP para `/api/invoice/documents/query` |
| `RATE_LIMIT_DOWNLOAD_PER_HOUR_TOKEN` | `50` | Límite de descargas por hora por token para PDF/XML |
| `RATE_LIMIT_DOWNLOAD_PER_HOUR_IP` | `200` | Límite de descargas por hora por IP para PDF/XML |
| `RATE_LIMIT_ENABLED` | `true` | Habilitar/deshabilitar rate limiting globalmente |

### Script SQL de Migración (Flyway)

**Archivo:** `src/main/resources/flyway/V202501XX__add_rate_limiting_params.sql`

```sql
-- Insertar parámetros de rate limiting
INSERT INTO params (id_param, code_param, value_param, description_param, status, creation_date)
VALUES 
    (NEXTVAL('params_seq'), 'RATE_LIMIT_QUERY_REQ_PER_MIN_TOKEN', '100', 
     'Límite de requests por minuto por token para consulta de documentos', TRUE, CURRENT_TIMESTAMP),
    
    (NEXTVAL('params_seq'), 'RATE_LIMIT_QUERY_REQ_PER_HOUR_IP', '500', 
     'Límite de requests por hora por IP para consulta de documentos', TRUE, CURRENT_TIMESTAMP),
    
    (NEXTVAL('params_seq'), 'RATE_LIMIT_DOWNLOAD_PER_HOUR_TOKEN', '50', 
     'Límite de descargas por hora por token para PDF/XML', TRUE, CURRENT_TIMESTAMP),
    
    (NEXTVAL('params_seq'), 'RATE_LIMIT_DOWNLOAD_PER_HOUR_IP', '200', 
     'Límite de descargas por hora por IP para PDF/XML', TRUE, CURRENT_TIMESTAMP),
    
    (NEXTVAL('params_seq'), 'RATE_LIMIT_ENABLED', 'true', 
     'Habilitar/deshabilitar rate limiting globalmente', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (code_param) DO NOTHING;
```

---

## 🔧 Implementación Técnica

### 1. Dependencias (pom.xml)

**No se requieren dependencias adicionales** - Redis ya está configurado con `spring-boot-starter-data-redis`.

### 2. RateLimitingConfig.java

```java
@Configuration
@EnableAspectJAutoProxy
public class RateLimitingConfig {
    
    @Bean
    public RedisTemplate<String, String> rateLimitRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
```

### 3. RateLimited.java (Anotación)

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    
    /**
     * Tipo de rate limiting a aplicar
     */
    RateLimitType type() default RateLimitType.QUERY;
    
    /**
     * Nombre del endpoint para logging
     */
    String endpoint() default "";
}

enum RateLimitType {
    QUERY,      // Para /api/invoice/documents/query
    DOWNLOAD    // Para /api/invoice/{id}/pdf y /api/invoice/{id}/xml
}
```

### 4. RateLimitingService.java

```java
public interface RateLimitingService {
    
    /**
     * Verifica si el request puede proceder según los límites configurados
     * 
     * @param token Token JWT del usuario (puede ser null)
     * @param ipAddress Dirección IP del cliente
     * @param rateLimitType Tipo de rate limiting (QUERY o DOWNLOAD)
     * @throws RateLimitExceededException Si se excede algún límite
     */
    void checkRateLimit(String token, String ipAddress, RateLimitType rateLimitType);
    
    /**
     * Obtiene información sobre los límites actuales
     */
    RateLimitInfo getRateLimitInfo(String token, String ipAddress, RateLimitType rateLimitType);
}
```

### 5. RateLimitingServiceImpl.java

**Estrategia de implementación:**

- **Redis Keys:**
  - `rate_limit:query:token:{token}:minute` → Contador por minuto por token
  - `rate_limit:query:ip:{ip}:hour` → Contador por hora por IP
  - `rate_limit:download:token:{token}:hour` → Contador por hora por token
  - `rate_limit:download:ip:{ip}:hour` → Contador por hora por IP

- **Algoritmo:** Token Bucket o Sliding Window Log
- **TTL:** Automático según la ventana de tiempo (60 seg para minuto, 3600 seg para hora)

### 6. RateLimitingAspect.java

```java
@Aspect
@Component
@Slf4j
public class RateLimitingAspect {
    
    private final RateLimitingService rateLimitingService;
    private final ParamsService paramsService;
    
    @Around("@annotation(rateLimited)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) 
            throws Throwable {
        
        // Verificar si rate limiting está habilitado
        boolean enabled = Boolean.parseBoolean(
            paramsService.getParamValueOrDefault("RATE_LIMIT_ENABLED", "true")
        );
        
        if (!enabled) {
            return joinPoint.proceed();
        }
        
        // Obtener token y IP del request
        HttpServletRequest request = getHttpServletRequest(joinPoint);
        String token = extractToken(request);
        String ipAddress = getClientIpAddress(request);
        
        // Verificar límites
        rateLimitingService.checkRateLimit(
            token, 
            ipAddress, 
            rateLimited.type()
        );
        
        // Si pasa, continuar con la ejecución
        return joinPoint.proceed();
    }
    
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        // Manejar proxies (X-Forwarded-For, X-Real-IP)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
```

### 7. RateLimitExceededException.java

```java
public class RateLimitExceededException extends RuntimeException {
    
    private final RateLimitInfo rateLimitInfo;
    
    public RateLimitExceededException(String message, RateLimitInfo rateLimitInfo) {
        super(message);
        this.rateLimitInfo = rateLimitInfo;
    }
    
    public RateLimitInfo getRateLimitInfo() {
        return rateLimitInfo;
    }
}
```

### 8. Modificación en InvoiceController.java

```java
@PostMapping("/documents/query")
@RateLimited(type = RateLimitType.QUERY, endpoint = "/api/invoice/documents/query")
public ResponseEntity<Page<DocumentSearchResponseDto>> queryDocuments(...) {
    // Código existente
}

@GetMapping("/{id}/pdf")
@RateLimited(type = RateLimitType.DOWNLOAD, endpoint = "/api/invoice/{id}/pdf")
public ResponseEntity<?> downloadPdf(@PathVariable("id") Long idInvoice) {
    // Código existente
}

@GetMapping("/{id}/xml")
@RateLimited(type = RateLimitType.DOWNLOAD, endpoint = "/api/invoice/{id}/xml")
public ResponseEntity<?> downloadXml(@PathVariable("id") Long idInvoice) {
    // Código existente
}
```

### 9. GlobalExceptionHandler (modificar existente o crear nuevo)

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException ex) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 429);
        response.put("status", "ERROR");
        response.put("message", ex.getMessage());
        response.put("rateLimitInfo", ex.getRateLimitInfo());
        
        HttpHeaders headers = new HttpHeaders();
        RateLimitInfo info = ex.getRateLimitInfo();
        headers.add("X-RateLimit-Limit", String.valueOf(info.getLimit()));
        headers.add("X-RateLimit-Remaining", String.valueOf(info.getRemaining()));
        headers.add("X-RateLimit-Reset", String.valueOf(info.getResetTime()));
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(response);
    }
}
```

---

## 📈 Estrategia de Rate Limiting

### Algoritmo: Sliding Window Log

**Ventajas:**
- Precisión exacta en las ventanas de tiempo
- Fácil implementación con Redis
- Permite tracking detallado

**Implementación con Redis:**

```java
// Ejemplo para límite por minuto
public boolean checkLimitPerMinute(String key, int limit) {
    long now = System.currentTimeMillis();
    long windowStart = now - 60000; // 1 minuto atrás
    
    // Agregar timestamp actual
    redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
    
    // Remover timestamps fuera de la ventana
    redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
    
    // Contar requests en la ventana
    Long count = redisTemplate.opsForZSet().count(key, windowStart, now);
    
    // Establecer TTL
    redisTemplate.expire(key, Duration.ofMinutes(2));
    
    return count <= limit;
}
```

### Alternativa: Token Bucket (más eficiente)

Si se prefiere mayor eficiencia, se puede usar Token Bucket:

```java
// Ejemplo simplificado
public boolean checkTokenBucket(String key, int capacity, int refillRate) {
    // Implementación con Redis Lua script para atomicidad
    // ...
}
```

---

## 🔍 Headers HTTP de Respuesta

Cuando se exceda un límite, se retornarán los siguientes headers:

```
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1704067200
Content-Type: application/json

{
  "code": 429,
  "status": "ERROR",
  "message": "Rate limit exceeded: 100 requests per minute by token",
  "rateLimitInfo": {
    "limit": 100,
    "remaining": 0,
    "resetTime": 1704067200,
    "limitType": "TOKEN",
    "timeWindow": "MINUTE"
  }
}
```

---

## 🧪 Testing

### Casos de Prueba

1. **Test de límite por token (query):**
   - Hacer 100 requests en 1 minuto → ✅ Debe pasar
   - Hacer 101 requests en 1 minuto → ❌ Debe retornar 429

2. **Test de límite por IP (query):**
   - Hacer 500 requests desde misma IP en 1 hora → ✅ Debe pasar
   - Hacer 501 requests desde misma IP en 1 hora → ❌ Debe retornar 429

3. **Test de límite por token (download):**
   - Hacer 50 descargas en 1 hora → ✅ Debe pasar
   - Hacer 51 descargas en 1 hora → ❌ Debe retornar 429

4. **Test de límite por IP (download):**
   - Hacer 200 descargas desde misma IP en 1 hora → ✅ Debe pasar
   - Hacer 201 descargas desde misma IP en 1 hora → ❌ Debe retornar 429

5. **Test de deshabilitación:**
   - Configurar `RATE_LIMIT_ENABLED=false` → ✅ No debe aplicar límites

6. **Test de diferentes tokens:**
   - Token A hace 100 requests → ✅ Pasa
   - Token B hace 100 requests → ✅ Pasa (límites independientes)

---

## 📝 Consideraciones Adicionales

### 1. Bypass para Roles Especiales

Si se requiere permitir bypass para ciertos roles (ej: SUPER_ADMIN):

```java
@Around("@annotation(rateLimited)")
public Object checkRateLimit(...) {
    // Verificar si el usuario tiene rol que permite bypass
    if (hasBypassRole()) {
        return joinPoint.proceed();
    }
    // ... resto del código
}
```

### 2. Logging y Monitoreo

- Registrar todos los casos donde se excede el límite
- Métricas para dashboard (ej: Prometheus)
- Alertas cuando se detecten patrones de abuso

### 3. Configuración Dinámica

- Los parámetros en `params` pueden actualizarse sin reiniciar la aplicación
- Considerar cachear valores de `params` con TTL corto (ej: 30 segundos)

### 4. Performance

- Usar scripts Lua de Redis para operaciones atómicas
- Minimizar llamadas a base de datos (cachear `params`)
- Considerar connection pooling para Redis

### 5. Escalabilidad

- Redis permite escalado horizontal
- Considerar Redis Cluster para alta disponibilidad

---

## 🚀 Plan de Implementación

### Fase 1: Infraestructura Base
1. ✅ Crear migración SQL para parámetros
2. ✅ Crear `RateLimitingConfig`
3. ✅ Crear `RateLimited` annotation
4. ✅ Crear `RateLimitExceededException`

### Fase 2: Servicio Core
1. ✅ Crear `RateLimitingService` interface
2. ✅ Implementar `RateLimitingServiceImpl` con Redis
3. ✅ Integrar con `ParamsService` para configuración

### Fase 3: Interceptación
1. ✅ Crear `RateLimitingAspect`
2. ✅ Implementar extracción de token e IP
3. ✅ Agregar anotaciones a endpoints en `InvoiceController`

### Fase 4: Manejo de Errores
1. ✅ Crear/modificar `GlobalExceptionHandler`
2. ✅ Implementar headers HTTP estándar (X-RateLimit-*)

### Fase 5: Testing
1. ✅ Tests unitarios para `RateLimitingService`
2. ✅ Tests de integración para endpoints
3. ✅ Tests de carga para verificar límites

### Fase 6: Documentación
1. ✅ Actualizar documentación de API
2. ✅ Documentar parámetros configurables
3. ✅ Guía de troubleshooting

---

## ⚠️ Riesgos y Mitigaciones

| Riesgo | Impacto | Mitigación |
|--------|--------|------------|
| Redis no disponible | Alto | Fallback: permitir todas las requests o usar in-memory cache temporal |
| Performance overhead | Medio | Optimizar con scripts Lua, cachear params |
| Falsos positivos (IP compartida) | Bajo | Documentar que límites por IP pueden afectar usuarios detrás de proxy |
| Configuración incorrecta | Medio | Validación de parámetros al iniciar, valores por defecto seguros |

---

## 📊 Métricas de Éxito

- ✅ Reducción de requests abusivos en >80%
- ✅ Tiempo de respuesta <10ms adicional por request
- ✅ Disponibilidad de Redis >99.9%
- ✅ Cero falsos positivos reportados

---

## 🔄 Mantenimiento

### Actualización de Límites

```sql
-- Ejemplo: Aumentar límite de consultas por token
UPDATE params 
SET value_param = '150', 
    update_date = CURRENT_TIMESTAMP 
WHERE code_param = 'RATE_LIMIT_QUERY_REQ_PER_MIN_TOKEN';
```

### Monitoreo

- Revisar logs de `RateLimitExceededException`
- Monitorear uso de Redis
- Ajustar límites según patrones de uso reales

---

## 📚 Referencias

- [RFC 6585 - 429 Too Many Requests](https://tools.ietf.org/html/rfc6585)
- [Redis Rate Limiting Patterns](https://redis.io/docs/manual/patterns/rate-limiting/)
- [Spring AOP Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop)

---

## 💡 Recomendaciones Adicionales

### 1. 🎯 Rate Limiting Diferenciado por Plan de Suscripción

**Contexto:** El sistema ya tiene planes de suscripción (FREE, BASIC, PRO, BUSINESS, ENTERPRISE). Los límites deberían variar según el plan.

**Implementación:**

```java
// Nuevos parámetros en BD
RATE_LIMIT_QUERY_FREE_PER_MIN_TOKEN = 50      // Plan FREE: más restrictivo
RATE_LIMIT_QUERY_BASIC_PER_MIN_TOKEN = 100    // Plan BASIC: estándar
RATE_LIMIT_QUERY_PRO_PER_MIN_TOKEN = 200      // Plan PRO: más permisivo
RATE_LIMIT_QUERY_BUSINESS_PER_MIN_TOKEN = 500 // Plan BUSINESS: muy permisivo
RATE_LIMIT_QUERY_ENTERPRISE_PER_MIN_TOKEN = -1 // Plan ENTERPRISE: ilimitado

// En RateLimitingService
public void checkRateLimit(String token, String ipAddress, Long idCompany, RateLimitType type) {
    // Obtener plan de suscripción de la empresa
    SubscriptionPlan plan = subscriptionService.getActivePlan(idCompany);
    
    // Obtener límite según plan
    int limit = getLimitForPlan(plan.getCode(), type);
    
    if (limit == -1) {
        return; // Ilimitado para ENTERPRISE
    }
    
    // Aplicar límite...
}
```

**Ventajas:**
- ✅ Incentiva upgrades de plan
- ✅ Mejor experiencia para clientes premium
- ✅ Protección más estricta para planes gratuitos

---

### 2. 🏢 Rate Limiting por Empresa (idCompany)

**Contexto:** Además de limitar por token e IP, limitar por empresa previene abuso desde múltiples usuarios de la misma empresa.

**Implementación:**

```java
// Nuevos límites
RATE_LIMIT_QUERY_PER_HOUR_COMPANY = 1000  // Límite global por empresa
RATE_LIMIT_DOWNLOAD_PER_HOUR_COMPANY = 500

// Redis Key adicional
rate_limit:query:company:{idCompany}:hour
rate_limit:download:company:{idCompany}:hour
```

**Casos de uso:**
- Empresa con múltiples usuarios compartiendo IP (ej: oficina)
- Prevenir abuso coordinado desde una empresa
- Control de costos por empresa

---

### 3. ⚡ Cache de Configuración de Params

**Problema:** Consultar BD en cada request es costoso.

**Solución:** Cachear valores de `params` con TTL corto.

```java
@Service
public class ParamsServiceImpl implements ParamsService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String CACHE_PREFIX = "params_cache:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    
    @Override
    public Optional<String> getParamValue(String codeParam) {
        // Intentar desde cache primero
        String cached = redisTemplate.opsForValue().get(CACHE_PREFIX + codeParam);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        // Si no está en cache, consultar BD
        Optional<String> value = paramsRepo
            .findByCodeParamAndStatusTrue(codeParam)
            .map(Params::getValueParam);
        
        // Guardar en cache
        value.ifPresent(v -> 
            redisTemplate.opsForValue().set(
                CACHE_PREFIX + codeParam, 
                v, 
                CACHE_TTL
            )
        );
        
        return value;
    }
}
```

**Beneficios:**
- ✅ Reduce carga en BD (de N requests a 1 cada 30 seg)
- ✅ Mejora latencia (<1ms vs 5-10ms)
- ✅ Permite actualización dinámica (TTL corto)

---

### 4. 🛡️ Graceful Degradation si Redis Falla

**Problema:** Si Redis está caído, no queremos bloquear todo el sistema.

**Solución:** Fallback a modo permisivo o in-memory cache.

```java
@Service
public class RateLimitingServiceImpl implements RateLimitingService {
    
    private boolean redisAvailable = true;
    
    @Override
    public void checkRateLimit(...) {
        try {
            // Intentar con Redis
            checkWithRedis(...);
            redisAvailable = true;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis no disponible, usando fallback permisivo");
            redisAvailable = false;
            
            // Opción 1: Permitir todas las requests (más permisivo)
            if (fallbackMode == FallbackMode.ALLOW_ALL) {
                return; // No bloquear
            }
            
            // Opción 2: Usar cache in-memory temporal (más seguro)
            if (fallbackMode == FallbackMode.IN_MEMORY) {
                checkWithInMemoryCache(...);
            }
        }
    }
}

// Configuración en params
RATE_LIMIT_FALLBACK_MODE = ALLOW_ALL  // o IN_MEMORY
```

**Estrategias:**
- **ALLOW_ALL:** Permite todas las requests (útil para desarrollo)
- **IN_MEMORY:** Usa ConcurrentHashMap local (limitado a una instancia)
- **DENY_ALL:** Bloquea todo (más seguro pero puede causar downtime)

---

### 5. 📋 Whitelist/Blacklist de IPs y Tokens

**Casos de uso:**
- Whitelist: IPs de confianza (ej: servidores internos, partners)
- Blacklist: IPs bloqueadas por abuso previo

**Implementación:**

```java
// Nuevos parámetros (JSON en value_param)
RATE_LIMIT_WHITELIST_IPS = ["192.168.1.0/24", "10.0.0.1"]
RATE_LIMIT_BLACKLIST_IPS = ["203.0.113.42"]
RATE_LIMIT_WHITELIST_TOKENS = ["token-special-1", "token-special-2"]

// En RateLimitingAspect
private boolean isWhitelisted(String ip, String token) {
    List<String> whitelistIPs = getWhitelistIPs();
    List<String> whitelistTokens = getWhitelistTokens();
    
    return whitelistIPs.contains(ip) || whitelistTokens.contains(token);
}

private boolean isBlacklisted(String ip, String token) {
    List<String> blacklistIPs = getBlacklistIPs();
    return blacklistIPs.contains(ip);
}
```

**Formato en BD:**
```json
// value_param para RATE_LIMIT_WHITELIST_IPS
["192.168.1.0/24", "10.0.0.1", "172.16.0.0/16"]
```

---

### 6. 📊 Métricas y Alertas

**Implementación con Micrometer/Prometheus:**

```java
@Service
public class RateLimitingServiceImpl {
    
    private final MeterRegistry meterRegistry;
    private final Counter rateLimitExceededCounter;
    private final Gauge rateLimitRemainingGauge;
    
    public RateLimitingServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.rateLimitExceededCounter = Counter.builder("rate_limit.exceeded")
            .tag("type", "query")
            .register(meterRegistry);
    }
    
    public void checkRateLimit(...) {
        if (exceeded) {
            rateLimitExceededCounter.increment(
                Tags.of("endpoint", endpoint, "ip", ipAddress)
            );
            
            // Alerta si se excede frecuentemente
            if (getExceededCountLastHour() > 100) {
                alertService.sendAlert("Rate limit abuse detected", ...);
            }
        }
    }
}
```

**Métricas a trackear:**
- `rate_limit.exceeded` - Contador de excedencias
- `rate_limit.remaining` - Requests restantes
- `rate_limit.check.duration` - Tiempo de verificación

---

### 7. 🔍 Rate Limiting en Otros Endpoints Críticos

**Endpoints adicionales a considerar:**

| Endpoint | Método | Razón | Límite Sugerido |
|----------|--------|-------|----------------|
| `/api/invoice` | POST | Creación masiva | 50 req/min por token |
| `/api/credit-note` | POST | Creación masiva | 30 req/min por token |
| `/api/debit-note` | POST | Creación masiva | 30 req/min por token |
| `/api/invoice/search-for-credit-note` | POST | Búsquedas | 100 req/min por token |
| `/api/invoice/search-for-debit-note` | POST | Búsquedas | 100 req/min por token |

**Implementación gradual:**
1. Fase 1: Solo los 3 endpoints críticos (propuesta actual)
2. Fase 2: Agregar endpoints de creación
3. Fase 3: Agregar endpoints de búsqueda

---

### 8. 📡 Headers Informativos en Todas las Respuestas

**Mejora UX:** Informar al cliente sobre límites incluso cuando no se exceden.

```java
@Aspect
public class RateLimitingAspect {
    
    @Around("@annotation(rateLimited)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) 
            throws Throwable {
        
        // Verificar límites
        RateLimitInfo info = rateLimitingService.getRateLimitInfo(...);
        
        // Ejecutar request
        Object result = joinPoint.proceed();
        
        // Agregar headers informativos a la respuesta
        if (result instanceof ResponseEntity) {
            HttpHeaders headers = ((ResponseEntity<?>) result).getHeaders();
            headers.add("X-RateLimit-Limit", String.valueOf(info.getLimit()));
            headers.add("X-RateLimit-Remaining", String.valueOf(info.getRemaining()));
            headers.add("X-RateLimit-Reset", String.valueOf(info.getResetTime()));
        }
        
        return result;
    }
}
```

**Headers en respuesta exitosa:**
```
HTTP/1.1 200 OK
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1704067260
```

**Beneficios:**
- ✅ Frontend puede mostrar progreso al usuario
- ✅ Mejor UX (ej: "87 requests restantes")
- ✅ Previene sorpresas al acercarse al límite

---

### 9. 🔄 Rate Limiting Adaptativo

**Concepto:** Ajustar límites según carga del sistema.

**Implementación:**

```java
// Nuevos parámetros
RATE_LIMIT_ADAPTIVE_ENABLED = true
RATE_LIMIT_ADAPTIVE_THRESHOLD_CPU = 80  // Si CPU > 80%, reducir límites 50%
RATE_LIMIT_ADAPTIVE_THRESHOLD_MEMORY = 85

// En RateLimitingService
private int getAdaptiveLimit(int baseLimit) {
    if (!isAdaptiveEnabled()) {
        return baseLimit;
    }
    
    double cpuUsage = getCpuUsage();
    double memoryUsage = getMemoryUsage();
    
    if (cpuUsage > getCpuThreshold() || memoryUsage > getMemoryThreshold()) {
        // Reducir límites en 50% bajo alta carga
        return (int) (baseLimit * 0.5);
    }
    
    return baseLimit;
}
```

**Ventajas:**
- ✅ Protege el sistema bajo alta carga
- ✅ Previene degradación de performance
- ✅ Auto-ajuste según condiciones

---

### 10. 🧪 Testing con Redis Testcontainers

**Mejora en tests:** Usar Redis real en tests de integración.

```java
@Testcontainers
class RateLimitingServiceIntegrationTest {
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    
    @Test
    void testRateLimitExceeded() {
        // Test con Redis real
        // ...
    }
}
```

---

### 11. 📝 Logging Estructurado

**Mejora:** Logs estructurados para análisis.

```java
@Slf4j
public class RateLimitingAspect {
    
    private void logRateLimitCheck(String token, String ip, RateLimitInfo info) {
        log.info(
            "Rate limit check: token={}, ip={}, limit={}, remaining={}, endpoint={}",
            maskToken(token),  // Enmascarar token por seguridad
            ip,
            info.getLimit(),
            info.getRemaining(),
            endpoint
        );
    }
    
    private void logRateLimitExceeded(String token, String ip, RateLimitInfo info) {
        log.warn(
            "Rate limit exceeded: token={}, ip={}, limit={}, endpoint={}",
            maskToken(token),
            ip,
            info.getLimit(),
            endpoint
        );
    }
}
```

**Formato JSON (opcional):**
```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "level": "WARN",
  "event": "RATE_LIMIT_EXCEEDED",
  "token": "***masked***",
  "ip": "192.168.1.100",
  "endpoint": "/api/invoice/documents/query",
  "limit": 100,
  "remaining": 0
}
```

---

### 12. 🔐 Seguridad: Enmascaramiento de Tokens en Logs

**Importante:** Nunca loggear tokens completos.

```java
private String maskToken(String token) {
    if (token == null || token.length() < 10) {
        return "***";
    }
    // Mostrar solo primeros 4 y últimos 4 caracteres
    return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
}
```

---

## 🎯 Priorización de Recomendaciones

### Alta Prioridad (Implementar en Fase 1)
1. ✅ **Cache de configuración de params** - Impacto directo en performance
2. ✅ **Graceful degradation** - Crítico para disponibilidad
3. ✅ **Headers informativos** - Mejora UX significativa

### Media Prioridad (Implementar en Fase 2)
4. ✅ **Rate limiting por plan** - Valor de negocio alto
5. ✅ **Rate limiting por empresa** - Protección adicional
6. ✅ **Métricas y alertas** - Observabilidad

### Baja Prioridad (Implementar en Fase 3)
7. ✅ **Whitelist/Blacklist** - Casos especiales
8. ✅ **Rate limiting adaptativo** - Optimización avanzada
9. ✅ **Otros endpoints** - Expansión gradual

---

## 📋 Checklist de Implementación Recomendada

### Fase 1: Core + Mejoras Críticas
- [ ] Implementación base (propuesta original)
- [ ] Cache de params (recomendación #3)
- [ ] Graceful degradation (recomendación #4)
- [ ] Headers informativos (recomendación #8)

### Fase 2: Diferenciación por Plan
- [ ] Rate limiting por plan (recomendación #1)
- [ ] Rate limiting por empresa (recomendación #2)
- [ ] Métricas básicas (recomendación #6)

### Fase 3: Optimizaciones y Expansión
- [ ] Whitelist/Blacklist (recomendación #5)
- [ ] Rate limiting en otros endpoints (recomendación #7)
- [ ] Rate limiting adaptativo (recomendación #9)

---

**Nota:** Esta es una propuesta. No se ha implementado código aún. Revisar y aprobar antes de proceder con la implementación.

