# ⚡ INICIO RÁPIDO - Autenticación de Tareas Programadas

## 🎯 Objetivo

Hacer que la tarea programada `executeSendInvoiceToSri()` funcione correctamente llamando al servicio de firma digital.

## 🚀 Opción Más Rápida (5 minutos)

### Si controlas el servicio de firma digital (ebill-customer):

**1. En el servicio de firma digital**, modifica `SecurityConfig.java` para aceptar headers internos:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        // Permitir llamadas internas con header X-Internal-Call
        .requestMatchers(request ->
            "true".equals(request.getHeader("X-Internal-Call"))
        ).permitAll()

        // Resto de configuración...
        .anyRequest().authenticated()
    );
    return http.build();
}
```

**2. Reinicia ambos servicios:**

```bash
# Servicio de firma digital
cd ebill-customer
./mvnw spring-boot:run

# Este servicio (ebill-billing)
cd ebill-billing
./mvnw spring-boot:run
```

**3. Verifica en los logs** (cada 2 minutos):

```
INFO  - Found X invoices pending to send to SRI
INFO  - Successfully processed invoice 123
```

✅ **¡Listo!** La tarea programada ahora funciona.

---

## 🔐 Opción con Token (Más segura)

### Si prefieres usar autenticación con token:

**1. Obtén un token del servicio de firma digital:**

```bash
curl -X POST http://localhost:8180/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tu-usuario","password":"tu-password"}'
```

**2. Copia el token de la respuesta y configúralo en:**

`src/main/resources/application-localhost.properties`:

```properties
scheduled.task.auth.token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.tu-token-aqui...
```

**3. Reinicia el servicio:**

```bash
./mvnw spring-boot:run
```

✅ **¡Listo!** La tarea usará el token automáticamente.

---

## 📋 ¿Qué hace la solución?

El interceptor `FeignScheduledTaskConfig` detecta automáticamente cuando una llamada Feign proviene de una tarea programada (sin usuario logueado) y agrega:

- **Con token configurado**: `Authorization: Bearer tu-token`
- **Sin token**: `X-Internal-Call: true` + `X-Caller: scheduled-task`

---

## 🔍 Verificación

Espera 2 minutos (la tarea se ejecuta cada 2 minutos) y revisa los logs:

### ✅ Funciona correctamente:

```
INFO  - Found 3 invoices pending to send to SRI
INFO  - Successfully processed invoice 101
INFO  - Successfully processed invoice 102
INFO  - Successfully processed invoice 103
```

### ❌ Aún falla (401):

```
ERROR - Unauthorized access to digital signature service for invoice 101
```

**Solución**: Verifica que el servicio de firma digital acepte headers internos o que el token esté correctamente configurado.

---

## 📚 Documentación Completa

Ver `CONFIGURACION-AUTH-SCHEDULED-TASKS.md` para:

- Configuración con API Key
- Troubleshooting
- Configuración para producción
- Detalles de seguridad

---

## ⚠️ Importante

**No commitear tokens reales en el código**. Usa variables de entorno en producción:

```properties
scheduled.task.auth.token=${SCHEDULED_TASK_TOKEN}
```

```bash
export SCHEDULED_TASK_TOKEN="tu-token-aqui"
```
