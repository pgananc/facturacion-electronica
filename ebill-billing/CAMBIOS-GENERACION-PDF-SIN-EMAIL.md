# Cambios: Generación de PDFs y XMLs sin Email

## Resumen

Se modifica el flujo para que **siempre se generen los PDFs y XMLs** de facturas y notas de crédito autorizadas, independientemente de si el cliente tiene correo electrónico. Si el cliente no tiene correo, simplemente se marca `emailSent = false` y no se envía la notificación.

## Cambios Propuestos

### 1. Modificar `InvoiceServiceImpl.notifyAuthorizedInvoice()`

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java`

**Líneas**: ~1322-1418

**Cambio**: Separar la generación de PDFs del envío de email. Siempre generar PDFs si está autorizado, pero solo enviar email si hay correo.

```java
private void notifyAuthorizedInvoice(
    InvoiceDto invoiceDto,
    Invoice savedInvoice,
    AuthorizationSri authorizationSri,
    ClientDto clientDto) {

  // Verificar si el correo ya fue enviado
  if (Boolean.TRUE.equals(savedInvoice.getEmailSent())) {
    log.info(
        "Email already sent for invoice {} on {}. Skipping notification.",
        savedInvoice.getIdInvoice(),
        savedInvoice.getEmailSentDate());
    return;
  }

  if (Objects.isNull(authorizationSri)
      || !StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
      || !"AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri())) {
    return;
  }

  if (!StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
    log.warn(
        "Authorization XML path is missing for invoice {}. Skipping email notification.",
        savedInvoice.getIdInvoice());
    return;
  }

  Path storageDir = null;
  try {
    storageDir = Paths.get(authorizationSri.getAuthorizationStoragePath());
  } catch (Exception ex) {
    log.warn(
        "Authorization XML path is invalid for invoice {}. Skipping email notification.",
        savedInvoice.getIdInvoice(),
        ex);
    return;
  }

  Path xmlPath = storageDir.resolve(authorizationSri.getAccessKey() + ".xml");
  if (!Files.isRegularFile(xmlPath)) {
    log.warn(
        "Authorization XML is unavailable for invoice {}. Skipping email notification.",
        savedInvoice.getIdInvoice());
    return;
  }

  // Usar el ClientDto pasado como parámetro (ya obtenido previamente)
  if (Objects.nonNull(clientDto)) {
    invoiceDto.setBuyerName(clientDto.getName());
    if (Objects.nonNull(clientDto.getContacts()) && !clientDto.getContacts().isEmpty()) {
      clientDto
          .getContacts()
          .forEach(
              c -> {
                if (Objects.nonNull(c.getContactType()) && c.getContactType().equals("MAI")) {
                  invoiceDto.setBuyerMail(c.getValue());
                }
              });
    }
  } else {
    // Fallback: si no se pasó el ClientDto, intentar obtenerlo (caso legacy)
    try {
      ClientDtoResponse response = customerClient.getClientById(savedInvoice.getIdClient());
      if (Objects.nonNull(response) && !CollectionUtils.isEmpty(response.getClientDtos())) {
        ClientDto fallbackClientDto = response.getClientDtos().get(0);
        invoiceDto.setBuyerName(fallbackClientDto.getName());
        if (Objects.nonNull(fallbackClientDto.getContacts())
            && !fallbackClientDto.getContacts().isEmpty()) {
          fallbackClientDto
              .getContacts()
              .forEach(
                  c -> {
                    if (Objects.nonNull(c.getContactType()) && c.getContactType().equals("MAI")) {
                      invoiceDto.setBuyerMail(c.getValue());
                    }
                  });
        }
      }
    } catch (feign.FeignException.Unauthorized e) {
      log.error(
          "Unauthorized access to digital signature service for invoice {} (retry {}/5) - Scheduled tasks need authentication configuration",
          savedInvoice.getIdInvoice(),
          savedInvoice.getRetryCount());
      handleInvoiceFailure(savedInvoice, "Unauthorized access to digital signature service");
    }
  }

  // CAMBIO: Siempre generar PDFs, independientemente del email
  // El servicio de delivery ahora maneja la generación de PDFs y el envío de email por separado
  invoiceDeliveryService.deliverAuthorizedInvoice(invoiceDto, savedInvoice, authorizationSri);

  // CAMBIO: Si no hay email, marcar como no enviado explícitamente
  String recipientEmail = resolveBuyerEmail(invoiceDto);
  if (!StringUtils.hasText(recipientEmail)) {
    log.info(
        "No recipient email found for invoice {}. PDF and XML generated, but email not sent. Marking emailSent as false.",
        savedInvoice.getIdInvoice());
    savedInvoice.setEmailSent(false);
    invoiceRepo.save(savedInvoice);
  }
}
```

### 2. Modificar `InvoiceDeliveryServiceImpl.deliverAuthorizedInvoice()`

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceDeliveryServiceImpl.java`

**Líneas**: ~32-113

**Cambio**: Separar la generación de PDFs del envío de email. Generar PDFs siempre, pero solo enviar email si hay correo.

```java
@Override
public void deliverAuthorizedInvoice(
    InvoiceDto invoiceDto, Invoice invoice, AuthorizationSri authorizationSri) {
  if (authorizationSri == null
      || !StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
    log.warn("Authorization XML path is missing for invoice {}. Skipping delivery.", invoice);
    return;
  }

  // Verificar si el correo ya fue enviado (solo para evitar reenvío de email)
  // Pero siempre generar PDFs si no se han generado
  boolean emailAlreadySent = Boolean.TRUE.equals(invoice.getEmailSent());

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

    // Obtener logoPath, regime y specialTaxpayerResolution desde CompanyDto si está disponible
    String logoPath = null;
    String regime = null;
    String specialTaxpayerResolution = null;

    if (invoiceDto.getCompanyDto() != null) {
      logoPath = invoiceDto.getCompanyDto().getLogoPath();
      regime = invoiceDto.getCompanyDto().getRegime();
      specialTaxpayerResolution = invoiceDto.getCompanyDto().getSpecialTaxpayerResolution();
    }

    // CAMBIO: Siempre generar PDF, independientemente del email
    Path pdfPath =
        invoicePdfService.generateAndSaveInvoicePdf(
            authorizationXml, outputDir, logoPath, regime, specialTaxpayerResolution);
    byte[] pdfBytes = Files.readAllBytes(pdfPath);

    log.info(
        "PDF generated successfully for invoice {} at path: {}",
        invoice.getIdInvoice(),
        pdfPath);

    // CAMBIO: Solo enviar email si hay correo y no se ha enviado antes
    String recipientEmail = invoiceDto.getBuyerMail();
    if (StringUtils.hasText(recipientEmail) && !emailAlreadySent) {
      Integer emailResult =
          invoiceEmailService.sendAuthorizedInvoice(
              recipientEmail,
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
            recipientEmail);
      } else {
        log.warn(
            "Email sending failed for invoice {} to recipient {}. Will retry on next notification attempt.",
            invoice.getIdInvoice(),
            recipientEmail);
      }
    } else if (!StringUtils.hasText(recipientEmail)) {
      log.info(
          "No recipient email found for invoice {}. PDF generated but email not sent.",
          invoice.getIdInvoice());
      // No marcar emailSent aquí, se marca en notifyAuthorizedInvoice
    } else {
      log.info(
          "Email already sent for invoice {} on {}. PDF generated but email skipped.",
          invoice.getIdInvoice(),
          invoice.getEmailSentDate());
    }

  } catch (Exception ex) {
    log.error(
        "Failed to deliver invoice {} to recipient {}",
        invoice != null ? invoice.getIdInvoice() : "unknown",
        invoiceDto != null ? invoiceDto.getBuyerMail() : "unknown",
        ex);
  }
}
```

### 3. Modificar `CreditNoteServiceImpl.notifyAuthorizedCreditNote()`

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/CreditNoteServiceImpl.java`

**Líneas**: ~376-429

**Cambio**: Similar a facturas, separar generación de PDFs del envío de email.

```java
private void notifyAuthorizedCreditNote(
    CreditNoteDto creditNoteDto, CreditNote savedCreditNote, AuthorizationSri authorizationSri) {

  // Verificar si el correo ya fue enviado
  if (Boolean.TRUE.equals(savedCreditNote.getEmailSent())) {
    log.info(
        "Email already sent for credit note {} on {}. Skipping notification.",
        savedCreditNote.getIdCreditNote(),
        savedCreditNote.getEmailSentDate());
    return;
  }

  if (Objects.isNull(authorizationSri)
      || !StringUtils.hasText(authorizationSri.getAuthorizationStatusSri())
      || !"AUTORIZADO".equalsIgnoreCase(authorizationSri.getAuthorizationStatusSri())) {
    return;
  }

  if (!StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
    log.warn(
        "Authorization XML path is missing for credit note {}. Skipping email notification.",
        savedCreditNote.getIdCreditNote());
    return;
  }

  // Obtener ClientDto completo con contactos
  ClientDto clientDtoWithContacts = null;
  try {
    if (creditNoteDto.getClientDto() != null
        && creditNoteDto.getClientDto().getIdClient() != null) {
      clientDtoWithContacts =
          customerClient
              .getClientById(creditNoteDto.getClientDto().getIdClient())
              .getClientDtos()
              .get(0);
      creditNoteDto.setClientDto(clientDtoWithContacts);
    }
  } catch (Exception e) {
    log.warn("Error loading client for credit note notification: {}", e.getMessage());
  }

  // CAMBIO: Siempre generar PDFs, independientemente del email
  // El servicio de delivery ahora maneja la generación de PDFs y el envío de email por separado
  creditNoteDeliveryService.deliverAuthorizedCreditNote(
      creditNoteDto, savedCreditNote, authorizationSri);

  // CAMBIO: Si no hay email, marcar como no enviado explícitamente
  String recipientEmail = getRecipientEmail(creditNoteDto, clientDtoWithContacts);
  if (!StringUtils.hasText(recipientEmail)) {
    log.info(
        "No recipient email found for credit note {}. PDF and XML generated, but email not sent. Marking emailSent as false.",
        savedCreditNote.getIdCreditNote());
    savedCreditNote.setEmailSent(false);
    creditNoteRepo.save(savedCreditNote);
  }
}
```

### 4. Modificar `CreditNoteDeliveryServiceImpl.deliverAuthorizedCreditNote()`

**Archivo**: `src/main/java/com/wallparisoft/ebillbilling/service/impl/CreditNoteDeliveryServiceImpl.java`

**Líneas**: ~32-120

**Cambio**: Separar la generación de PDFs del envío de email. Generar PDFs siempre, pero solo enviar email si hay correo.

```java
@Override
public void deliverAuthorizedCreditNote(
    CreditNoteDto creditNoteDto, CreditNote creditNote, AuthorizationSri authorizationSri) {
  if (authorizationSri == null
      || !StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
    log.warn(
        "Authorization XML path is missing for credit note {}. Skipping delivery.",
        creditNote != null ? creditNote.getIdCreditNote() : "unknown");
    return;
  }

  // Verificar si el correo ya fue enviado (solo para evitar reenvío de email)
  // Pero siempre generar PDFs si no se han generado
  boolean emailAlreadySent = Boolean.TRUE.equals(creditNote.getEmailSent());

  try {
    Path storageDir = resolveStorageDir(authorizationSri);
    Path xmlPath = storageDir.resolve(authorizationSri.getAccessKey() + ".xml");
    if (!Files.isRegularFile(xmlPath)) {
      log.warn(
          "Authorization XML file could not be read for credit note {}. Skipping delivery.",
          creditNote != null ? creditNote.getIdCreditNote() : "unknown");
      return;
    }

    byte[] authorizationXmlBytes = Files.readAllBytes(xmlPath);
    String authorizationXml = new String(authorizationXmlBytes, StandardCharsets.UTF_8);

    Path outputDir = storageDir;
    Files.createDirectories(outputDir);

    // Obtener logoPath, regime y specialTaxpayerResolution desde CompanyDto si está disponible
    String logoPath = null;
    String regime = null;
    String specialTaxpayerResolution = null;

    if (creditNoteDto.getCompanyDto() != null) {
      logoPath = creditNoteDto.getCompanyDto().getLogoPath();
      regime = creditNoteDto.getCompanyDto().getRegime();
      specialTaxpayerResolution = creditNoteDto.getCompanyDto().getSpecialTaxpayerResolution();
    }

    // CAMBIO: Siempre generar PDF, independientemente del email
    Path pdfPath =
        creditNotePdfService.generateAndSaveCreditNotePdf(
            authorizationXml, outputDir, logoPath, regime, specialTaxpayerResolution);
    byte[] pdfBytes = Files.readAllBytes(pdfPath);

    log.info(
        "PDF generated successfully for credit note {} at path: {}",
        creditNote.getIdCreditNote(),
        pdfPath);

    // CAMBIO: Solo enviar email si hay correo y no se ha enviado antes
    String recipientEmail = getRecipientEmail(creditNoteDto);
    if (StringUtils.hasText(recipientEmail) && !emailAlreadySent) {
      Integer emailResult =
          creditNoteEmailService.sendAuthorizedCreditNote(
              recipientEmail,
              creditNote,
              creditNoteDto,
              authorizationSri,
              pdfBytes,
              authorizationXmlBytes);

      // Marcar como enviado solo si el correo se envió exitosamente (result = 1)
      if (emailResult != null && emailResult == 1) {
        creditNote.setEmailSent(true);
        creditNote.setEmailSentDate(LocalDateTime.now());
        creditNoteRepo.save(creditNote);
        log.info(
            "Email sent successfully for credit note {} to recipient {}. Marked as sent.",
            creditNote.getIdCreditNote(),
            recipientEmail);
      } else {
        log.warn(
            "Email sending failed for credit note {} to recipient {}. Will retry on next notification attempt.",
            creditNote.getIdCreditNote(),
            recipientEmail);
      }
    } else if (!StringUtils.hasText(recipientEmail)) {
      log.info(
          "No recipient email found for credit note {}. PDF generated but email not sent.",
          creditNote.getIdCreditNote());
      // No marcar emailSent aquí, se marca en notifyAuthorizedCreditNote
    } else {
      log.info(
          "Email already sent for credit note {} on {}. PDF generated but email skipped.",
          creditNote.getIdCreditNote(),
          creditNote.getEmailSentDate());
    }

  } catch (Exception ex) {
    log.error(
        "Failed to deliver credit note {} to recipient {}",
        creditNote != null ? creditNote.getIdCreditNote() : "unknown",
        creditNoteDto != null ? getRecipientEmail(creditNoteDto) : "unknown",
        ex);
  }
}
```

## Resumen de Cambios

### Comportamiento Actual

- ❌ Si no hay email del cliente, NO se generan PDFs
- ❌ Si no hay email del cliente, NO se guardan XMLs (aunque los XMLs ya se guardan en `SendInvoiceImpl`)
- ❌ El campo `emailSent` queda en `null` si no hay email

### Comportamiento Propuesto

- ✅ **Siempre se generan PDFs** cuando la factura/nota de crédito está autorizada
- ✅ **Siempre se guardan XMLs** (ya se hace en `SendInvoiceImpl`, no requiere cambios)
- ✅ Si no hay email, se marca `emailSent = false` explícitamente
- ✅ Solo se envía el email si hay correo del cliente

## Notas Importantes

1. **Los XMLs ya se generan y guardan** en `SendInvoiceImpl.authorizeXMLBySri()` y `SendCreditNoteImpl`, por lo que no requieren cambios adicionales.

2. **Los PDFs se generan en los servicios de delivery** (`InvoiceDeliveryServiceImpl` y `CreditNoteDeliveryServiceImpl`), por lo que estos son los que requieren modificación.

3. **El campo `emailSent`** debe marcarse explícitamente como `false` cuando no hay email, en lugar de dejarlo en `null`.

4. **Los logs** deben indicar claramente cuando se generan PDFs sin enviar email.

## Archivos a Modificar

1. `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceServiceImpl.java` - Método `notifyAuthorizedInvoice()`
2. `src/main/java/com/wallparisoft/ebillbilling/service/impl/InvoiceDeliveryServiceImpl.java` - Método `deliverAuthorizedInvoice()`
3. `src/main/java/com/wallparisoft/ebillbilling/service/impl/CreditNoteServiceImpl.java` - Método `notifyAuthorizedCreditNote()`
4. `src/main/java/com/wallparisoft/ebillbilling/service/impl/CreditNoteDeliveryServiceImpl.java` - Método `deliverAuthorizedCreditNote()`

