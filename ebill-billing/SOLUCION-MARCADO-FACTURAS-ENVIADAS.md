# Solución: Marcar Facturas Autorizadas y Enviadas por Correo

## Objetivo

Marcar las facturas que ya fueron autorizadas por el SRI y enviadas por correo electrónico, para evitar envíos duplicados y poder rastrear el estado de notificación.

---

## Análisis del Flujo Actual

1. **Autorización**: `InvoiceServiceImpl.saveInvoice()` → `sendInvoiceService.sendInvoiceSri()` → factura autorizada
2. **Notificación**: `notifyAuthorizedInvoice()` → `invoiceDeliveryService.deliverAuthorizedInvoice()` → `invoiceEmailService.sendAuthorizedInvoice()`
3. **Resultado**: `sendAuthorizedInvoice()` retorna `1` si se envió exitosamente, `0` si falló

---

## Solución Propuesta

### 1. Agregar Campos a la Entidad Invoice

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/entity/Invoice.java`

```java
@Column(name = "email_sent")
private Boolean emailSent;

@Column(name = "email_sent_date")
private LocalDateTime emailSentDate;
```

**Campos:**

- `emailSent` (Boolean): Indica si el correo fue enviado exitosamente
- `emailSentDate` (LocalDateTime): Fecha y hora en que se envió el correo

---

### 2. Migración Flyway

**Archivo:** `src/main/resources/flyway/V2025111803__add_email_sent_fields.sql`

```sql
-- Agregar campos para rastrear el envío de correos
ALTER TABLE invoice
    ADD COLUMN email_sent BOOLEAN DEFAULT FALSE,
    ADD COLUMN email_sent_date TIMESTAMP;

-- Crear índice para consultas rápidas de facturas no enviadas
CREATE INDEX IF NOT EXISTS idx_invoice_email_sent ON invoice(email_sent, id_authorization_sri);

-- Actualizar facturas existentes que ya tienen autorización
-- (asumiendo que si tienen autorización, probablemente ya se enviaron)
UPDATE invoice
SET email_sent = TRUE,
    email_sent_date = authorization_sri.authorization_date
FROM authorization_sri
WHERE invoice.id_authorization_sri = authorization_sri.id_authorization_sri
  AND authorization_sri.authorization_status_sri = 'AUTORIZADO'
  AND invoice.email_sent IS NULL;
```

---

### 3. Modificar InvoiceDeliveryServiceImpl

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceDeliveryServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceDeliveryServiceImpl implements InvoiceDeliveryService {

  private final InvoicePdfService invoicePdfService;
  private final InvoiceEmailService invoiceEmailService;
  private final ParamsService paramsService;
  private final IInvoiceRepo invoiceRepo; // ← AGREGAR

  @Override
  public void deliverAuthorizedInvoice(
      InvoiceDto invoiceDto, Invoice invoice, AuthorizationSri authorizationSri) {
    if (authorizationSri == null
        || !StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
      log.warn("Authorization XML path is missing for invoice {}. Skipping delivery.", invoice);
      return;
    }

    // Verificar si el correo ya fue enviado
    if (Boolean.TRUE.equals(invoice.getEmailSent())) {
      log.info("Email already sent for invoice {} on {}. Skipping delivery.",
          invoice.getIdInvoice(), invoice.getEmailSentDate());
      return;
    }

    try {
      Path storageDir = resolveStorageDir(authorizationSri);
      Path xmlPath = storageDir.resolve(authorizationSri.getAccessKey() + ".xml");
      if (!Files.isRegularFile(xmlPath)) {
        log.warn(
            "Authorization XML file could not be read for invoice {}. Skipping delivery.",
            invoice != null ? invoice.getIdInvoice() : "unknown");
        return;
      }

      byte[] authorizationXmlBytes = Files.readAllBytes(xmlPath);
      String authorizationXml = new String(authorizationXmlBytes, StandardCharsets.UTF_8);

      Path outputDir = storageDir;
      Files.createDirectories(outputDir);

      Path pdfPath = invoicePdfService.generateAndSaveInvoicePdf(authorizationXml, outputDir);
      byte[] pdfBytes = Files.readAllBytes(pdfPath);

      Integer emailResult = invoiceEmailService.sendAuthorizedInvoice(
          invoiceDto.getBuyerMail(),
          invoice,
          invoiceDto,
          authorizationSri,
          pdfBytes,
          authorizationXmlBytes);

      // Marcar como enviado solo si el correo se envió exitosamente (result = 1)
      if (emailResult != null && emailResult == 1) {
        invoice.setEmailSent(true);
        invoice.setEmailSentDate(LocalDateTime.now());
        invoiceRepo.save(invoice);
        log.info(
            "Email sent successfully for invoice {} to recipient {}. Marked as sent.",
            invoice.getIdInvoice(),
            invoiceDto.getBuyerMail());
      } else {
        log.warn(
            "Email sending failed for invoice {} to recipient {}. Will retry on next notification attempt.",
            invoice.getIdInvoice(),
            invoiceDto.getBuyerMail());
      }

    } catch (Exception ex) {
      log.error(
          "Failed to deliver invoice {} to recipient {}",
          invoice != null ? invoice.getIdInvoice() : "unknown",
          invoiceDto != null ? invoiceDto.getBuyerMail() : "unknown",
          ex);
    }
  }

  // ... resto del código sin cambios
}
```

**Cambios:**

- Agregar `IInvoiceRepo` como dependencia
- Verificar `emailSent` antes de enviar
- Actualizar `emailSent` y `emailSentDate` solo si el correo se envió exitosamente (result = 1)
- Guardar la factura actualizada en la BD

---

### 4. Modificar InvoiceServiceImpl (Opcional - Prevención)

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java`

En el método `notifyAuthorizedInvoice()`, agregar verificación temprana:

```java
private void notifyAuthorizedInvoice(
    InvoiceDto invoiceDto,
    Invoice savedInvoice,
    AuthorizationSri authorizationSri,
    ClientDto clientDto) {

  // Verificar si el correo ya fue enviado
  if (Boolean.TRUE.equals(savedInvoice.getEmailSent())) {
    log.info("Email already sent for invoice {} on {}. Skipping notification.",
        savedInvoice.getIdInvoice(), savedInvoice.getEmailSentDate());
    return;
  }

  if (Objects.isNull(authorizationSri)
      || !StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
      || !"AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri())) {
    return;
  }

  // ... resto del código sin cambios
}
```

**Beneficio:** Evita procesamiento innecesario si el correo ya fue enviado.

---

### 5. Consultas Útiles para el Repositorio (Opcional)

**Archivo:** `src/main/java/com/wallparisoft/ebillbilling/repository/IInvoiceRepo.java`

```java
/**
 * Busca facturas autorizadas que aún no han sido notificadas por correo
 */
@Query("SELECT i FROM Invoice i WHERE " +
       "i.authorizationSri.authorizationStatusSri = 'AUTORIZADO' AND " +
       "(i.emailSent IS NULL OR i.emailSent = false) AND " +
       "i.status = true")
List<Invoice> findAuthorizedInvoicesNotSentByEmail();

/**
 * Busca facturas autorizadas que ya fueron notificadas por correo
 */
@Query("SELECT i FROM Invoice i WHERE " +
       "i.authorizationSri.authorizationStatusSri = 'AUTORIZADO' AND " +
       "i.emailSent = true AND " +
       "i.status = true")
List<Invoice> findAuthorizedInvoicesSentByEmail();
```

---

## Resumen de Archivos a Modificar

### Nuevos Archivos:

1. ✅ `src/main/resources/flyway/V2025111803__add_email_sent_fields.sql`

### Archivos a Modificar:

1. ✅ `src/main/java/com/wallparisoft/ebillbilling/entity/Invoice.java` (agregar campos)
2. ✅ `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceDeliveryServiceImpl.java` (marcar como enviado)
3. ✅ `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java` (verificación opcional)
4. ✅ `src/main/java/com/wallparisoft/ebillbilling/repository/IInvoiceRepo.java` (consultas opcionales)

---

## Beneficios

1. **Evita envíos duplicados**: Verifica si el correo ya fue enviado antes de intentar enviarlo
2. **Rastreabilidad**: Permite saber cuándo se envió cada factura
3. **Consultas útiles**: Facilita reportes de facturas enviadas/no enviadas
4. **Reintentos seguros**: Si falla el envío, se puede reintentar sin marcar como enviado
5. **Auditoría**: Historial de cuándo se notificó cada factura

---

## Consideraciones

1. **Facturas existentes**: La migración marca como enviadas las facturas que ya tienen autorización (asumiendo que ya se enviaron)
2. **Reintentos**: Si el envío falla (`result = 0`), no se marca como enviado, permitiendo reintentos
3. **Transaccionalidad**: El guardado de la factura se hace después del envío exitoso para evitar marcar como enviado si falla
4. **Performance**: El índice en `email_sent` mejora las consultas de facturas no enviadas

---

## Flujo Completo

```
1. Factura autorizada por SRI
   ↓
2. notifyAuthorizedInvoice() verifica emailSent = false/null
   ↓
3. deliverAuthorizedInvoice() verifica emailSent = false/null
   ↓
4. Genera PDF y envía correo
   ↓
5. Si sendAuthorizedInvoice() retorna 1 (éxito):
   - invoice.setEmailSent(true)
   - invoice.setEmailSentDate(LocalDateTime.now())
   - invoiceRepo.save(invoice)
   ↓
6. Próxima vez: Verificación en paso 2 o 3 evita reenvío
```

---

## Notas Adicionales

- Si se necesita reenviar un correo manualmente, se puede actualizar `emailSent = false` en la BD
- El campo `emailSentDate` permite generar reportes de facturas enviadas en un rango de fechas
- Se puede agregar un endpoint administrativo para reenviar correos si es necesario
