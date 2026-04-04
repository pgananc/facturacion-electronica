# Solución: Error 401 Unauthorized en Tarea Programada

## Problema

La tarea programada (scheduled task) `executeSendInvoiceToSri()` está fallando con error 401 Unauthorized al intentar llamar al servicio de firma digital:

```
feign.FeignException$Unauthorized: [401] during [GET] to [http://localhost:8180/api/digital-signature/company/1]
```

## Causa

Las **tareas programadas** (scheduled tasks) se ejecutan en un contexto sin autenticación. No tienen:

- Token JWT
- Session de usuario
- Headers de autenticación

Cuando la tarea intenta llamar al servicio de firma digital vía Feign, el servicio rechaza la petición porque no hay credenciales.

## Solución Implementada: Manejo de Errores

Se mejoró el manejo de excepciones para capturar y registrar este error específico:

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java`

```java
@Override
public void executeSendInvoiceToSri() {
  List<Invoice> invoices =
      this.repo.findInvoicesExcludingSpecificSriStatuses("RECIBIDA", "AUTORIZADO");
  log.info("Found {} invoices pending to send to SRI", invoices.size());

  invoices.forEach(
      invoice -> {
        try {
          DigitalSignatureResponse digitalSignatureResponse =
              digitalSignatureClient.getDigitalSignatureByCompanyId(invoice.getIdCompany());

          if (digitalSignatureResponse == null || digitalSignatureResponse.getDigitalSignatureDto() == null) {
            log.warn("Digital signature not found for company {} - Invoice {}",
                invoice.getIdCompany(), invoice.getIdInvoice());
            return;
          }

          AuthorizationSri authorizationSri =
              sendInvoiceService.sendInvoiceSri(
                  serviceMapper.toInvoiceDto(invoice),
                  digitalSignatureResponse.getDigitalSignatureDto());
          invoice.setAuthorizationSri(authorizationSri);

          log.info("Successfully processed invoice {}", invoice.getIdInvoice());

        } catch (feign.FeignException.Unauthorized e) {
          // ← Nuevo: Captura específica de errores 401
          log.error("Unauthorized access to digital signature service for invoice {} - Scheduled tasks need authentication configuration",
              invoice.getIdInvoice());

        } catch (ResourceAccessException | feign.RetryableException e) {
          // ← Nuevo: Errores de conexión
          log.error("Connection error to digital signature service for invoice {}: {}",
              invoice.getIdInvoice(), e.getMessage());

        } catch (CertificateException | SAXException | IOException | JAXBException | InterruptedException e) {
          log.error("Process error for invoice {}: {}", invoice.getIdInvoice(), e.getMessage(), e);

        } catch (Exception e) {
          // ← Nuevo: Catch-all para errores inesperados
          log.error("Unexpected error processing invoice {}: {}", invoice.getIdInvoice(), e.getMessage(), e);
        }
      });
}
```

## Mejoras Aplicadas

1. ✅ **Captura de error 401 específico**: `feign.FeignException.Unauthorized`
2. ✅ **Mejor logging**: Mensajes descriptivos con IDs de facturas
3. ✅ **Validación de respuesta null**: Previene NullPointerException
4. ✅ **No rompe el scheduler**: Captura todas las excepciones para que la tarea siga ejecutándose
5. ✅ **Catch-all**: Captura cualquier excepción inesperada

## Soluciones Permanentes (Opcionales)

### Opción 1: Configurar Feign Interceptor para Scheduled Tasks

Crear un interceptor que agregue autenticación para llamadas desde tareas programadas:

```java
@Component
public class FeignScheduledTaskInterceptor implements RequestInterceptor {

  @Value("${scheduled.task.api.key:default-api-key}")
  private String apiKey;

  @Override
  public void apply(RequestTemplate template) {
    // Solo aplica si no hay autenticación existente
    if (!template.headers().containsKey("Authorization")) {
      template.header("X-API-Key", apiKey);
      // O usar un token específico para tareas programadas
      // template.header("Authorization", "Bearer " + scheduledTaskToken);
    }
  }
}
```

### Opción 2: Service Account / API Key

En el servicio de firma digital, permitir acceso con API Key para tareas programadas:

```java
// En el servicio de firma digital
@GetMapping("/api/digital-signature/company/{id}")
public DigitalSignatureResponse get(@PathVariable Long id,
                                    @RequestHeader(required = false) String apiKey) {
  // Validar API key para scheduled tasks
  if (apiKey != null && isValidApiKey(apiKey)) {
    // Permitir acceso
  }
  // ... resto de la lógica
}
```

### Opción 3: Deshabilitar Autenticación para Endpoints Internos

Si los servicios están en la misma red interna:

```java
// En SecurityConfig del servicio de firma digital
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  http
    .authorizeHttpRequests(auth -> auth
      .requestMatchers("/api/digital-signature/**")
        .hasAnyRole("USER", "SCHEDULED_TASK")
      .anyRequest().authenticated()
    );
  return http.build();
}
```

### Opción 4: Endpoint Específico sin Autenticación

Crear un endpoint separado para tareas programadas:

```java
// En el servicio de firma digital
@GetMapping("/internal/digital-signature/company/{id}")
@PreAuthorize("permitAll()")  // Sin autenticación
public DigitalSignatureResponse getForScheduledTask(@PathVariable Long id) {
  // Validar que la llamada viene de IP interna
  return digitalSignatureService.getByCompanyId(id);
}
```

Y cambiar el Feign client para usar este endpoint en scheduled tasks.

## Configuración de Tarea Programada

Revisar cuándo se ejecuta la tarea:

```java
@Scheduled(cron = "0 */5 * * * *")  // Cada 5 minutos
public void executeTask() {
  invoiceService.executeSendInvoiceToSri();
}
```

## Logs Esperados

Con la solución implementada, ahora verás logs claros:

```
INFO  - Found 5 invoices pending to send to SRI
ERROR - Unauthorized access to digital signature service for invoice 123 - Scheduled tasks need authentication configuration
ERROR - Unauthorized access to digital signature service for invoice 124 - Scheduled tasks need authentication configuration
...
```

En lugar del stack trace completo.

## Recomendaciones

1. **Corto plazo**: Usar la solución de manejo de errores (ya implementada)
2. **Mediano plazo**: Implementar Opción 1 o 2 (API Key/Service Account)
3. **Largo plazo**: Revisar arquitectura para que scheduled tasks tengan credenciales adecuadas

## Testing

### Test Manual

1. Detener la aplicación
2. Reiniciar para que tome los cambios
3. Esperar a que se ejecute la tarea programada
4. Verificar logs:
   ```
   Found X invoices pending to send to SRI
   Unauthorized access to digital signature service for invoice Y - Scheduled tasks need authentication configuration
   ```

### Deshabilitar Temporalmente la Tarea

Si quieres deshabilitar la tarea programada mientras implementas una solución permanente:

```java
// En application.properties
spring.task.scheduling.enabled=false
```

O comentar el `@Scheduled`:

```java
// @Scheduled(cron = "...")
public void executeTask() {
  invoiceService.executeSendInvoiceToSri();
}
```

## Archivos Modificados

- `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java`
  - Mejorado manejo de excepciones
  - Agregada captura específica de `feign.FeignException.Unauthorized`
  - Mejorado logging
