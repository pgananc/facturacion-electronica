# Configuración de Autenticación para Tareas Programadas

## Problema Resuelto

Las tareas programadas (scheduled tasks) necesitan llamar a servicios externos (como el servicio de firma digital) pero no tienen contexto de autenticación como los requests normales de usuarios.

## Solución Implementada

Se creó un interceptor de Feign que automáticamente agrega autenticación cuando detecta que la llamada proviene de una tarea programada (sin contexto de usuario).

## Archivos Creados/Modificados

1. **`src/main/java/com/wallparisoft/ebillbilling/config/FeignScheduledTaskConfig.java`** - Interceptor de autenticación
2. **`src/main/resources/application-localhost.properties`** - Configuración de credenciales

## Cómo Funciona

El interceptor detecta automáticamente si hay un contexto de autenticación:

```java
if (authentication == null || !authentication.isAuthenticated()) {
  // Agregar autenticación para scheduled task
}
```

### Opción 1: Token JWT (Recomendado)

1. **Obtener un token JWT** haciendo login en el servicio de firma digital:

```bash
curl -X POST http://localhost:8180/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "scheduled-task-user",
    "password": "your-password"
  }'
```

2. **Configurar el token** en `application-localhost.properties`:

```properties
scheduled.task.auth.token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

3. El interceptor agregará automáticamente el header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Nota**: Los tokens JWT expiran. Opciones:

- Usar un token con expiración larga (ej: 1 año)
- Implementar un mecanismo de refresh automático
- Usar API Key en su lugar

### Opción 2: API Key

Si el servicio de firma digital soporta API Keys:

1. **Configurar la API Key** en `application-localhost.properties`:

```properties
scheduled.task.auth.api-key=your-secure-api-key-here
```

2. El interceptor agregará automáticamente el header:

```
X-API-Key: your-secure-api-key-here
```

3. **En el servicio de firma digital**, validar el API Key:

```java
@GetMapping("/api/digital-signature/company/{id}")
public DigitalSignatureResponse get(
    @PathVariable Long id,
    @RequestHeader(value = "X-API-Key", required = false) String apiKey) {

  // Validar API Key
  if (apiKey != null && isValidApiKey(apiKey)) {
    // Permitir acceso
    return service.getByCompanyId(id);
  }

  // Continuar con autenticación normal
  // ...
}
```

### Opción 3: Headers Internos (Fallback)

Si no configuras ni token ni API Key, el interceptor agregará:

```
X-Internal-Call: true
X-Caller: scheduled-task
```

**Requerimiento**: El servicio de firma digital debe configurarse para aceptar estos headers:

```java
// En SecurityConfig del servicio de firma digital
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  http
    .authorizeHttpRequests(auth -> auth
      .requestMatchers(request ->
        "true".equals(request.getHeader("X-Internal-Call")))
        .permitAll()
      .anyRequest().authenticated()
    );
  return http.build();
}
```

## Configuración Recomendada

### Para Desarrollo Local

```properties
# application-localhost.properties

# Opción más simple: Headers internos
# (Requiere configurar el servicio de firma digital)

# O usar un token de prueba
scheduled.task.auth.token=your-dev-token-here
```

### Para Producción

```properties
# application.properties

# Usar API Key segura
scheduled.task.auth.api-key=${SCHEDULED_TASK_API_KEY}

# O token con expiración larga
scheduled.task.auth.token=${SCHEDULED_TASK_TOKEN}
```

Y configurar la variable de entorno en el servidor:

```bash
export SCHEDULED_TASK_API_KEY="your-secure-production-api-key"
```

## Testing

### 1. Sin Configuración (Headers Internos)

Dejar comentadas las configuraciones de token y API key:

```properties
# scheduled.task.auth.token=
# scheduled.task.auth.api-key=
```

El interceptor agregará `X-Internal-Call: true`.

**Logs esperados:**

```
DEBUG - No authentication context found, adding scheduled task credentials
DEBUG - Added internal call headers for scheduled task
```

**Si falla** (401), necesitas configurar el servicio de firma digital para aceptar estos headers.

### 2. Con Token JWT

Descomentar y configurar:

```properties
scheduled.task.auth.token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Logs esperados:**

```
DEBUG - No authentication context found, adding scheduled task credentials
DEBUG - Added Bearer token for scheduled task
INFO  - Successfully processed invoice 123
```

### 3. Con API Key

Descomentar y configurar:

```properties
scheduled.task.auth.api-key=my-secure-api-key-12345
```

**Logs esperados:**

```
DEBUG - No authentication context found, adding scheduled task credentials
DEBUG - Added API Key for scheduled task
INFO  - Successfully processed invoice 123
```

## Verificación

1. **Reiniciar la aplicación** para que tome la nueva configuración

2. **Verificar que el interceptor está registrado:**

```
Espera en los logs al inicio:
"Bean scheduledTaskAuthInterceptor created"
```

3. **Esperar a que se ejecute la tarea programada** (cada 2 minutos por defecto)

4. **Revisar los logs:**

Sin autenticación (antes):

```
ERROR - Unauthorized access to digital signature service for invoice 123
```

Con autenticación (después):

```
INFO  - Found 5 invoices pending to send to SRI
INFO  - Successfully processed invoice 123
INFO  - Successfully processed invoice 124
...
```

## Solución Rápida (Para Empezar Inmediatamente)

Si necesitas que funcione YA y el servicio de firma digital está en tu control:

1. **En el servicio de firma digital**, temporalmente deshabilita la autenticación para llamadas internas:

```java
// En SecurityConfig
.requestMatchers("/api/digital-signature/**").permitAll()
```

2. O acepta el header `X-Internal-Call`:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  http
    .authorizeHttpRequests(auth -> auth
      .requestMatchers(request -> {
        String internalCall = request.getHeader("X-Internal-Call");
        return "true".equals(internalCall);
      })
      .permitAll()
      .anyRequest().authenticated()
    );
  return http.build();
}
```

3. **Reinicia ambos servicios**

4. La tarea programada debería funcionar automáticamente

## Troubleshooting

### Sigue recibiendo 401

1. Verifica que el interceptor se está ejecutando:

   - Activa logs DEBUG: `logging.level.com.wallparisoft.ebillbilling.config=DEBUG`
   - Busca: "No authentication context found" en los logs

2. Verifica que el servicio de firma digital recibe los headers:

   - En el servicio de firma digital, agrega logging del request

3. Verifica la configuración:
   - `scheduled.task.auth.token` está correctamente copiado (sin espacios extra)
   - El token no ha expirado

### El interceptor no se ejecuta

Verifica que el bean se creó:

```java
@Autowired
private RequestInterceptor scheduledTaskAuthInterceptor;

@PostConstruct
public void init() {
  log.info("Scheduled task auth interceptor: {}", scheduledTaskAuthInterceptor);
}
```

## Seguridad

⚠️ **Importante:**

1. **Nunca commitear** tokens o API keys reales en el código
2. **Usar variables de entorno** en producción
3. **Rotar las credenciales** periódicamente
4. **Limitar los permisos** del usuario/token de scheduled tasks

Ejemplo de `.gitignore`:

```
application-localhost.properties
*.env
*.key
```

Y usar `application-localhost.properties.example` como plantilla:

```properties
# scheduled.task.auth.token=YOUR_TOKEN_HERE
# scheduled.task.auth.api-key=YOUR_API_KEY_HERE
```

## Próximos Pasos

1. ✅ **Inmediato**: Configura una de las 3 opciones de autenticación
2. **Corto plazo**: Implementa refresh automático de tokens si usas JWT
3. **Mediano plazo**: Migra a API Keys para mayor estabilidad
4. **Largo plazo**: Considera OAuth2 Client Credentials Flow para comunicación entre servicios
