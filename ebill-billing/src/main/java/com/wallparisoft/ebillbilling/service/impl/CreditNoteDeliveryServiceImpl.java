package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.CreditNoteDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.CreditNote;
import com.wallparisoft.ebillbilling.repository.ICreditNoteRepo;
import com.wallparisoft.ebillbilling.service.CreditNoteDeliveryService;
import com.wallparisoft.ebillbilling.service.CreditNoteEmailService;
import com.wallparisoft.ebillbilling.service.CreditNotePdfService;
import com.wallparisoft.ebillbilling.service.ParamsService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditNoteDeliveryServiceImpl implements CreditNoteDeliveryService {

  private final CreditNotePdfService creditNotePdfService;
  private final CreditNoteEmailService creditNoteEmailService;
  private final ParamsService paramsService;
  private final ICreditNoteRepo creditNoteRepo;

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
      log.info(
          "Starting PDF generation for credit note {} with XML path: {}",
          creditNote.getIdCreditNote(),
          xmlPath);
      Path pdfPath =
          creditNotePdfService.generateAndSaveCreditNotePdf(
              authorizationXml, outputDir, logoPath, regime, specialTaxpayerResolution);

      if (pdfPath == null || !Files.exists(pdfPath)) {
        log.error(
            "PDF generation failed for credit note {}. PDF path is null or file does not exist: {}",
            creditNote.getIdCreditNote(),
            pdfPath);
        return;
      }

      byte[] pdfBytes = Files.readAllBytes(pdfPath);

      log.info(
          "PDF generated successfully for credit note {} at path: {} (size: {} bytes)",
          creditNote.getIdCreditNote(),
          pdfPath,
          pdfBytes.length);

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

  private String getRecipientEmail(CreditNoteDto creditNoteDto) {
    if (creditNoteDto == null) {
      log.debug("CreditNoteDto is null, cannot get recipient email");
      return null;
    }

    // Intentar obtener email del cliente desde contactos (tipo "MAI" según el estándar)
    if (creditNoteDto.getClientDto() != null
        && creditNoteDto.getClientDto().getContacts() != null) {
      String emailFromContacts =
          creditNoteDto.getClientDto().getContacts().stream()
              .filter(contact -> "MAI".equalsIgnoreCase(contact.getContactType()))
              .filter(
                  contact ->
                      contact.getStatus() == null || Boolean.TRUE.equals(contact.getStatus()))
              .findFirst()
              .map(contact -> contact.getValue())
              .orElse(null);

      if (StringUtils.hasText(emailFromContacts)) {
        log.debug(
            "Found recipient email from client contacts for credit note: {}", emailFromContacts);
        return emailFromContacts;
      }
    }

    // Intentar obtener email usando reflexión por si el JSON tiene buyerMail aunque no esté en el
    // DTO
    try {
      java.lang.reflect.Method getBuyerMail = creditNoteDto.getClass().getMethod("getBuyerMail");
      Object buyerMail = getBuyerMail.invoke(creditNoteDto);
      if (buyerMail instanceof String && StringUtils.hasText((String) buyerMail)) {
        log.debug("Found recipient email from buyerMail field for credit note: {}", buyerMail);
        return (String) buyerMail;
      }
    } catch (Exception e) {
      // buyerMail no existe en el DTO, continuar
      log.trace("buyerMail field not found in CreditNoteDto, trying other methods");
    }

    log.debug(
        "No recipient email found for credit note. ClientDto: {}, Contacts: {}",
        creditNoteDto.getClientDto() != null,
        creditNoteDto.getClientDto() != null && creditNoteDto.getClientDto().getContacts() != null
            ? creditNoteDto.getClientDto().getContacts().size()
            : 0);
    return null;
  }

  private Path resolveStorageDir(AuthorizationSri authorizationSri) {
    if (authorizationSri != null
        && StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
      try {
        return Paths.get(authorizationSri.getAuthorizationStoragePath());
      } catch (Exception ignored) {
        // fallback to configured directory
      }
    }
    try {
      String baseDir = paramsService.getCreditNoteBaseDir();
      return Paths.get(baseDir);
    } catch (Exception ex) {
      throw new IllegalStateException(
          "No se pudo resolver el directorio base de almacenamiento", ex);
    }
  }
}
