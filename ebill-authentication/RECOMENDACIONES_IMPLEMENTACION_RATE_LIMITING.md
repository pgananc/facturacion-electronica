# Recomendaciones para Aplicar y Revisar Rate Limiting

## 📋 Estrategia de Implementación Paso a Paso

### Fase 1: Preparación y Configuración de Base de Datos

#### 1.1 Crear Migración Flyway
- ✅ Crear archivo de migración con el script SQL propuesto
- ✅ Nombre sugerido: `V2025010200__add_rate_limiting_params.sql` (ajustar fecha)
- ✅ Verificar que no haya conflictos con IDs existentes usando `COALESCE(MAX(id_param), 0) + 1`

**Revisión:**
```sql
-- Verificar que los parámetros se insertaron correctamente
SELECT code_param, value_param, description_param 
FROM public.params 
WHERE code_param LIKE '%RATE_LIMIT%' 
ORDER BY code_param;
```

#### 1.2 Agregar Enums al ParmsAuthEnum
**Ubicación:** Librería externa `com.wallparisoft.ebill.utils.enums.ParmsAuthEnum`

**Códigos a agregar:**
```java
// Mail Send Rate Limiting
COD010_MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS("MAIL_SEND_RATE_LIMIT_EMAIL_MAX_REQUESTS"),
COD011_MAIL_SEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES("MAIL_SEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES"),
COD012_MAIL_SEND_RATE_LIMIT_IP_MAX_REQUESTS("MAIL_SEND_RATE_LIMIT_IP_MAX_REQUESTS"),
COD013_MAIL_SEND_RATE_LIMIT_IP_WINDOW_MINUTES("MAIL_SEND_RATE_LIMIT_IP_WINDOW_MINUTES"),

// Token Client Rate Limiting
COD014_TOKEN_CLIENT_RATE_LIMIT_EMAIL_MAX_REQUESTS("TOKEN_CLIENT_RATE_LIMIT_EMAIL_MAX_REQUESTS"),
COD015_TOKEN_CLIENT_RATE_LIMIT_EMAIL_WINDOW_MINUTES("TOKEN_CLIENT_RATE_LIMIT_EMAIL_WINDOW_MINUTES"),
COD016_TOKEN_CLIENT_RATE_LIMIT_IP_MAX_REQUESTS("TOKEN_CLIENT_RATE_LIMIT_IP_MAX_REQUESTS"),
COD017_TOKEN_CLIENT_RATE_LIMIT_IP_WINDOW_MINUTES("TOKEN_CLIENT_RATE_LIMIT_IP_WINDOW_MINUTES"),

// Token Validate Rate Limiting
COD018_TOKEN_VALIDATE_RATE_LIMIT_IP_MAX_ATTEMPTS("TOKEN_VALIDATE_RATE_LIMIT_IP_MAX_ATTEMPTS"),
COD019_TOKEN_VALIDATE_RATE_LIMIT_IP_WINDOW_MINUTES("TOKEN_VALIDATE_RATE_LIMIT_IP_WINDOW_MINUTES"),
COD020_TOKEN_VALIDATE_RATE_LIMIT_CLIENT_MAX_ATTEMPTS("TOKEN_VALIDATE_RATE_LIMIT_CLIENT_MAX_ATTEMPTS"),
COD021_TOKEN_VALIDATE_RATE_LIMIT_CLIENT_WINDOW_MINUTES("TOKEN_VALIDATE_RATE_LIMIT_CLIENT_WINDOW_MINUTES"),

// Token Resend Rate Limiting
COD022_TOKEN_RESEND_RATE_LIMIT_EMAIL_MAX_REQUESTS("TOKEN_RESEND_RATE_LIMIT_EMAIL_MAX_REQUESTS"),
COD023_TOKEN_RESEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES("TOKEN_RESEND_RATE_LIMIT_EMAIL_WINDOW_MINUTES"),
COD024_TOKEN_RESEND_RATE_LIMIT_IP_MAX_REQUESTS("TOKEN_RESEND_RATE_LIMIT_IP_MAX_REQUESTS"),
COD025_TOKEN_RESEND_RATE_LIMIT_IP_WINDOW_MINUTES("TOKEN_RESEND_RATE_LIMIT_IP_WINDOW_MINUTES");
```

**Revisión:**
- Verificar que todos los códigos estén en el enum
- Compilar el proyecto y verificar que no haya errores

---

### Fase 2: Crear Servicios Genéricos de Rate Limiting

#### 2.1 Crear Servicio Genérico de Rate Limiting

**Recomendación:** Crear un servicio genérico reutilizable que maneje diferentes dimensiones (email, IP, cliente).

**Archivo:** `src/main/java/com/wallparisoft/authentication/service/GenericRateLimitService.java`

**Características:**
- Métodos genéricos para validar e incrementar contadores
- Soporte para múltiples dimensiones (email, IP, cliente)
- Configuración dinámica desde base de datos
- Manejo de TTL en Redis

**Estructura propuesta:**
```java
@Service
public class GenericRateLimitService {
    
    public enum RateLimitDimension {
        EMAIL, IP, CLIENT
    }
    
    /**
     * Valida si se puede realizar una solicitud según el rate limit
     * @param endpoint Código del endpoint (ej: "MAIL_SEND")
     * @param dimension Dimensión del rate limit (EMAIL, IP, CLIENT)
     * @param identifier Identificador único (email, IP, o clientNumber)
     * @return true si puede realizar la solicitud
     */
    public boolean canMakeRequest(String endpoint, RateLimitDimension dimension, String identifier);
    
    /**
     * Incrementa el contador de rate limiting
     */
    public void incrementRequest(String endpoint, RateLimitDimension dimension, String identifier);
    
    /**
     * Obtiene el tiempo restante hasta el próximo intento
     */
    public long getTimeUntilNextAttempt(String endpoint, RateLimitDimension dimension, String identifier);
    
    /**
     * Obtiene el número de intentos actuales
     */
    public int getCurrentAttempts(String endpoint, RateLimitDimension dimension, String identifier);
}
```

#### 2.2 Utilidad para Extraer IP

**Recomendación:** Crear una clase utilitaria para extraer IP del request, reutilizando el código existente.

**Archivo:** `src/main/java/com/wallparisoft/authentication/util/RequestUtils.java`

```java
public class RequestUtils {
    public static String extractClientIp(HttpServletRequest request) {
        // Reutilizar lógica de LoginController/SessionController
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        
        return request.getRemoteAddr();
    }
}
```

**Revisión:**
- Refactorizar `LoginController` y `SessionController` para usar esta utilidad
- Verificar que funciona correctamente con proxies y load balancers

---

### Fase 3: Implementar Rate Limiting por Endpoint

#### 3.1 `/api/mail/send` - Reset Password

**Archivo:** `src/main/java/com/wallparisoft/authentication/service/impl/UserServiceImpl.java`

**Cambios necesarios:**
1. Inyectar `GenericRateLimitService` y `HttpServletRequest` (o crear un DTO con IP)
2. Validar rate limiting por email ANTES de procesar
3. Validar rate limiting por IP ANTES de procesar
4. Incrementar contadores después de enviar email exitosamente
5. Lanzar `RateLimitExceededException` si se excede el límite

**Puntos de atención:**
- ⚠️ El método `sendMailUser` actualmente solo recibe `String mail`
- 💡 **Solución:** Modificar el controlador para pasar `HttpServletRequest` o crear un DTO que incluya IP

**Revisión:**
```java
// Test: Intentar enviar 11 emails desde el mismo email en menos de 1 hora
// Esperado: El 11vo debe fallar con RateLimitExceededException

// Test: Intentar enviar 11 emails desde la misma IP en menos de 1 hora
// Esperado: El 11vo debe fallar con RateLimitExceededException
```

#### 3.2 `/api/token/client` - Consulta Facturas

**Archivo:** `src/main/java/com/wallparisoft/authentication/service/impl/ClientTokenServiceImpl.java`

**Cambios necesarios:**
1. Extender el rate limiting actual para incluir validación por IP
2. Mantener la lógica existente por email/identification
3. Validar AMBOS límites (email e IP) y aplicar el más restrictivo

**Estrategia:**
- Validar límite por email (ya existe)
- Validar límite por IP (nuevo)
- Si cualquiera excede, lanzar excepción
- Incrementar ambos contadores

**Revisión:**
```java
// Test: 5 solicitudes desde el mismo email, diferentes IPs
// Esperado: La 6ta debe fallar (límite por email)

// Test: 20 solicitudes desde la misma IP, diferentes emails
// Esperado: La 21va debe fallar (límite por IP)
```

#### 3.3 `/api/token/validate-client/generate` - Validación Token

**Archivo:** `src/main/java/com/wallparisoft/authentication/service/impl/ClientTokenServiceImpl.java`

**Cambios necesarios:**
1. **NUEVO:** Implementar rate limiting por IP (10 intentos / 15 min)
2. **NUEVO:** Implementar rate limiting por cliente (20 intentos / 1 hora)
3. Validar AMBOS límites antes de procesar
4. Incrementar contadores en intentos fallidos
5. NO incrementar en intentos exitosos (o decrementar como actualmente)

**Puntos de atención:**
- ⚠️ Actualmente NO hay rate limiting en este endpoint
- ⚠️ Es crítico para prevenir fuerza bruta
- 💡 Considerar incrementar contador solo en fallos, no en éxitos

**Revisión:**
```java
// Test: 10 intentos fallidos desde la misma IP en 15 minutos
// Esperado: El 11vo debe fallar con RateLimitExceededException

// Test: 20 intentos fallidos desde el mismo cliente en 1 hora
// Esperado: El 21vo debe fallar con RateLimitExceededException

// Test: Validación exitosa NO debe incrementar contador
```

#### 3.4 `/api/token/resend-client` - Reenvío Token

**Archivo:** `src/main/java/com/wallparisoft/authentication/service/impl/ClientTokenServiceImpl.java`

**Cambios necesarios:**
1. Extender el rate limiting actual para incluir validación por IP
2. Mantener la lógica existente por email
3. Validar AMBOS límites (email e IP)

**Revisión:**
```java
// Test: 3 reenvíos desde el mismo email en 1 hora
// Esperado: El 4to debe fallar (límite por email)

// Test: 10 reenvíos desde la misma IP en 1 hora
// Esperado: El 11vo debe fallar (límite por IP)
```

---

### Fase 4: Actualizar Controladores

#### 4.1 MailController

**Cambios:**
```java
@PostMapping(value = "/send", consumes = MediaType.TEXT_PLAIN_VALUE)
public ResponseEntity<Integer> send(
    @RequestBody String mail, 
    HttpServletRequest request) {  // ← Agregar HttpServletRequest
    
    String clientIp = RequestUtils.extractClientIp(request);
    // Pasar IP al servicio
    result = userService.sendMailUser(mail, clientIp);
    // ...
}
```

#### 4.2 TokenController

**Cambios:**
- Agregar `HttpServletRequest` a los métodos que necesitan IP
- Pasar IP a los servicios correspondientes

---

### Fase 5: Mejoras en RateLimitExceededException

**Recomendación:** Extender la excepción para incluir información sobre qué dimensión excedió el límite.

```java
public class RateLimitExceededException extends RuntimeException {
    private final RateLimitDimension exceededDimension;
    private final String identifier; // email, IP, o clientNumber
    
    // ... campos existentes ...
}
```

---

## 🔍 Checklist de Revisión

### Antes de Implementar

- [ ] Revisar propuesta de parámetros con el equipo
- [ ] Validar valores de rate limits con negocio
- [ ] Verificar que Redis esté configurado y funcionando
- [ ] Revisar estructura de la tabla `params` en base de datos

### Durante la Implementación

- [ ] Crear migración Flyway y probarla en ambiente de desarrollo
- [ ] Agregar enums al ParmsAuthEnum
- [ ] Crear servicios genéricos de rate limiting
- [ ] Implementar rate limiting endpoint por endpoint
- [ ] Agregar logs informativos para debugging
- [ ] Manejar errores gracefully (valores por defecto si falla lectura de params)

### Después de Implementar

#### Pruebas Unitarias
- [ ] Test: Rate limiting por email funciona correctamente
- [ ] Test: Rate limiting por IP funciona correctamente
- [ ] Test: Rate limiting por cliente funciona correctamente
- [ ] Test: Múltiples dimensiones se validan correctamente
- [ ] Test: TTL en Redis se establece correctamente
- [ ] Test: Contadores se resetean después del TTL

#### Pruebas de Integración
- [ ] Test: `/api/mail/send` respeta límites por email e IP
- [ ] Test: `/api/token/client` respeta límites por email e IP
- [ ] Test: `/api/token/validate-client/generate` respeta límites por IP y cliente
- [ ] Test: `/api/token/resend-client` respeta límites por email e IP
- [ ] Test: Mensajes de error son claros y útiles

#### Pruebas de Carga
- [ ] Verificar que Redis puede manejar el volumen esperado
- [ ] Medir impacto en latencia de las validaciones
- [ ] Verificar que no hay memory leaks en Redis

#### Pruebas de Seguridad
- [ ] Intentar bypass de rate limiting cambiando IP
- [ ] Intentar bypass de rate limiting cambiando email
- [ ] Verificar que proxies/load balancers no afectan la detección de IP
- [ ] Verificar que rate limiting funciona con IPv6

### Monitoreo y Observabilidad

- [ ] Agregar métricas para rate limit hits
- [ ] Agregar logs cuando se excede un límite
- [ ] Configurar alertas para patrones sospechosos
- [ ] Dashboard para visualizar rate limiting en tiempo real

---

## 🚨 Consideraciones Importantes

### 1. Orden de Validación
**Recomendación:** Validar rate limiting ANTES de procesar la solicitud para evitar consumo innecesario de recursos.

```java
// ✅ CORRECTO
if (!rateLimitService.canMakeRequest(...)) {
    throw new RateLimitExceededException(...);
}
// Procesar solicitud...

// ❌ INCORRECTO
// Procesar solicitud...
if (!rateLimitService.canMakeRequest(...)) {
    throw new RateLimitExceededException(...);
}
```

### 2. Manejo de Errores
**Recomendación:** Si falla la lectura de parámetros desde BD, usar valores por defecto seguros (más restrictivos).

```java
try {
    maxRequests = getMaxRequestsFromParams();
} catch (Exception e) {
    log.warn("Error leyendo params, usando valor por defecto", e);
    maxRequests = 5; // Valor conservador
}
```

### 3. Performance
**Recomendación:** Cachear parámetros en memoria con TTL corto (ej: 5 minutos) para evitar consultas constantes a BD.

```java
@Cacheable(value = "rateLimitParams", key = "#codeParam")
public Params getParam(String codeParam) {
    return paramsRepository.findParamsByCodeParam(codeParam);
}
```

### 4. Testing
**Recomendación:** Crear tests que verifiquen:
- Límites se respetan correctamente
- Contadores se incrementan correctamente
- TTL funciona correctamente
- Múltiples dimensiones funcionan en conjunto

### 5. Rollback Plan
**Revisión:** Tener un plan de rollback:
- Script SQL para eliminar parámetros si es necesario
- Deshabilitar rate limiting mediante feature flag
- Valores por defecto que permitan operación normal

---

## 📊 Métricas Recomendadas

### Métricas a Monitorear
1. **Rate Limit Hits:** Número de veces que se excede un límite
2. **Rate Limit por Endpoint:** Distribución de rate limits por endpoint
3. **Rate Limit por Dimensión:** Email vs IP vs Cliente
4. **Tiempo de Respuesta:** Impacto del rate limiting en latencia
5. **Uso de Redis:** Memoria y conexiones utilizadas

### Alertas Recomendadas
1. **Alta frecuencia de rate limit hits:** Posible ataque
2. **Rate limit hits concentrados en una IP:** Posible abuso
3. **Redis sin memoria:** Necesita escalamiento
4. **Errores al leer parámetros:** Problema de configuración

---

## 🔄 Estrategia de Deployment

### Desarrollo
1. Implementar en ambiente de desarrollo
2. Probar exhaustivamente
3. Ajustar valores si es necesario

### Staging
1. Deploy con valores de producción
2. Pruebas de carga
3. Monitoreo activo

### Producción
1. Deploy gradual (feature flag)
2. Monitoreo intensivo las primeras 24 horas
3. Ajustes finos según comportamiento real

---

## 📝 Notas Finales

1. **Valores Configurables:** Todos los límites deben ser configurables desde BD sin redeploy
2. **Flexibilidad:** Diseñar para fácil extensión a otros endpoints
3. **Documentación:** Documentar cada parámetro y su propósito
4. **Comunicación:** Informar a usuarios sobre rate limits en mensajes de error
5. **Revisión Continua:** Revisar y ajustar límites periódicamente según uso real

---

## 🛠️ Herramientas Útiles

### Para Testing
- **Postman/Insomnia:** Para probar endpoints manualmente
- **Redis CLI:** Para inspeccionar claves y TTL
- **JMeter/Gatling:** Para pruebas de carga

### Para Monitoreo
- **Redis Monitoring:** RedisInsight o similar
- **Application Metrics:** Prometheus + Grafana
- **Logs:** ELK Stack o similar

---

## ✅ Criterios de Aceptación

La implementación se considera completa cuando:

1. ✅ Todos los parámetros están en la base de datos
2. ✅ Todos los endpoints tienen rate limiting implementado
3. ✅ Rate limiting funciona para todas las dimensiones (email, IP, cliente)
4. ✅ Tests unitarios e integración pasan
5. ✅ Documentación está actualizada
6. ✅ Monitoreo está configurado
7. ✅ Valores están ajustados según uso real
8. ✅ No hay degradación de performance significativa

