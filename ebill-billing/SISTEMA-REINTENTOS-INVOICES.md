# Sistema de Reintentos para Facturas

## Resumen

Se implementó un sistema de reintentos automáticos para facturas que fallan al enviarse al SRI, con desactivación automática después de 5 intentos fallidos.

## Cambios Realizados

### 1. Entidad Invoice

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/entity/Invoice.java`

Se agregó el campo `retryCount` para rastrear el número de intentos de envío:

```java
@Column(name = "retry_count")
private Integer retryCount;
```

### 2. Migración de Base de Datos

**Archivo**: `src/main/resources/flyway/V2025101402__add_retry_count.sql`

```sql
ALTER TABLE invoice
    ADD COLUMN retry_count INTEGER DEFAULT 0;

UPDATE invoice SET retry_count = 0 WHERE retry_count IS NULL;
```

### 3. Repositorio IInvoiceRepo

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/repository/IInvoiceRepo.java`

Se modificó la consulta para **filtrar solo facturas activas** (`status = true`):

```java
@Query(
    "SELECT i FROM Invoice i WHERE "
        + "i.status = true AND "  // ← NUEVO FILTRO
        + "((i.authorizationSri.receptionStatusSri IS NULL OR i.authorizationSri.receptionStatusSri <> :receptionStatus) "
        + "OR (i.authorizationSri.authorizationStatusSri IS NULL OR i.authorizationSri.authorizationStatusSri <> :authorizationStatus))")
List<Invoice> findInvoicesExcludingSpecificSriStatuses(
    @Param("receptionStatus") String receptionStatus,
    @Param("authorizationStatus") String authorizationStatus);
```

### 4. Servicio InvoiceServiceImpl

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java`

#### 4.1. Inicialización en `saveInvoice`

Cuando se crea una nueva factura, el `retryCount` se inicializa en 0:

```java
invoice.setRetryCount(0); // Inicializar contador de reintentos
```

#### 4.2. Lógica de Reintentos en `executeSendInvoiceToSri`

La tarea programada ahora implementa el sistema de reintentos:

**Flujo:**

1. **Inicialización**: Si `retryCount` es null, se inicializa en 0
2. **Verificación de límite**: Si `retryCount >= 5`, se marca la factura y sus detalles como inactivos
3. **Intento de envío**: Se intenta enviar la factura al SRI
4. **Éxito**: Si tiene éxito, se resetea `retryCount` a 0
5. **Fallo**: Si falla, se incrementa `retryCount` y se guarda la factura
6. **Máximo de reintentos**: Al llegar a 5 reintentos, se marca como inactiva

#### 4.3. Método `handleInvoiceFailure`

Maneja cada fallo incrementando el contador:

```java
private void handleInvoiceFailure(Invoice invoice, String reason) {
  invoice.setRetryCount(invoice.getRetryCount() + 1);

  if (invoice.getRetryCount() >= 5) {
    log.error("Invoice {} has exceeded maximum retries ({}) - Marking as inactive. Reason: {}", ...);
    deactivateInvoiceAndDetails(invoice);
  } else {
    log.warn("Invoice {} retry {}/5. Reason: {}", ...);
    repo.save(invoice);
  }
}
```

#### 4.4. Método `deactivateInvoiceAndDetails`

Marca la factura y todos sus detalles como inactivos:

```java
private void deactivateInvoiceAndDetails(Invoice invoice) {
  invoice.setStatus(false);

  if (invoice.getInvoiceDetails() != null) {
    invoice.getInvoiceDetails().forEach(detail -> detail.setStatus(false));
  }

  repo.save(invoice);
  log.info("Invoice {} and its {} details marked as inactive", ...);
}
```

## Comportamiento del Sistema

### Escenarios

#### 1. Factura Nueva

- `retryCount` = 0
- `status` = true
- La tarea programada intenta enviarla al SRI

#### 2. Primer Fallo

- `retryCount` se incrementa a 1
- `status` permanece en true
- Log: `Invoice X retry 1/5. Reason: [error]`
- En la próxima ejecución de la tarea (2 minutos después), se reintenta

#### 3. Fallos Subsiguientes (2, 3, 4)

- `retryCount` se incrementa a 2, 3, 4
- `status` permanece en true
- Se sigue reintentando cada 2 minutos

#### 4. Quinto Fallo

- `retryCount` se incrementa a 5
- `status` se marca como **false**
- Todos los `invoiceDetails` también se marcan como **false**
- Log: `Invoice X has exceeded maximum retries (5) - Marking as inactive`
- **La factura ya NO será procesada** en futuras ejecuciones de la tarea

#### 5. Éxito en Cualquier Reintento

- `retryCount` se resetea a 0
- `status` permanece en true
- La factura se marca como procesada exitosamente

## Logs Mejorados

Todos los mensajes de error ahora incluyen el contador de reintentos:

```
ERROR - Unauthorized access to digital signature service for invoice 2 (retry 1/5)
ERROR - Connection error to digital signature service for invoice 3 (retry 2/5): Connection refused
ERROR - Process error for invoice 4 (retry 3/5): Certificate expired
ERROR - Unexpected error processing invoice 5 (retry 4/5): Null pointer exception
ERROR - Invoice 6 has exceeded maximum retries (5) - Marking as inactive. Reason: Connection error
INFO  - Invoice 6 and its 3 details marked as inactive
```

## Reactivación Manual

Si necesitas reactivar una factura que fue marcada como inactiva después de 5 reintentos:

### SQL Directo

```sql
-- Reactivar una factura específica y sus detalles
UPDATE invoice SET status = true, retry_count = 0 WHERE id_invoice = X;
UPDATE invoice_detail SET status = true WHERE id_invoice = X;
```

### Consideraciones

- Solo reactivar facturas si el problema subyacente (firma digital, conectividad, etc.) fue resuelto
- Al resetear `retry_count` a 0, la factura tendrá otros 5 intentos

## Configuración de la Tarea Programada

La tarea se ejecuta cada 2 minutos (configurado en `application.properties`):

```properties
scheduled.task.cron=0 */2 * * * *
```

## Beneficios

1. ✅ **No procesa facturas inactivas**: Reduce carga innecesaria
2. ✅ **Reintentos automáticos**: Hasta 5 intentos para transitorios errores
3. ✅ **Desactivación automática**: Después de 5 fallos, evita intentos infinitos
4. ✅ **Trazabilidad**: Logs detallados con contador de reintentos
5. ✅ **Integridad de datos**: Marca factura y detalles como inactivos
6. ✅ **Reset automático**: Si tiene éxito después de un fallo, resetea el contador

## Próximos Pasos (Opcional)

1. **Dashboard**: Crear endpoint para listar facturas con reintentos fallidos
2. **Notificaciones**: Enviar alertas cuando una factura alcance el límite de reintentos
3. **Reintentos exponenciales**: Aumentar el tiempo entre reintentos (2min, 5min, 10min, etc.)
4. **Reintentos por tipo de error**: Diferentes estrategias según el error (conexión vs certificado)
