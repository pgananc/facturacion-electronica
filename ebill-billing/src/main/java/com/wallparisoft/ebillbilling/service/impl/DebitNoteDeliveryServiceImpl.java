package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.DebitNoteDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.DebitNote;
import com.wallparisoft.ebillbilling.repository.IDebitNoteRepo;
import com.wallparisoft.ebillbilling.service.DebitNoteDeliveryService;
import com.wallparisoft.ebillbilling.service.DebitNoteEmailService;
import com.wallparisoft.ebillbilling.service.DebitNotePdfService;
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
public class DebitNoteDeliveryServiceImpl implements DebitNoteDeliveryService {

  private final DebitNotePdfService debitNotePdfService;
  private final DebitNoteEmailService debitNoteEmailService;
  private final ParamsService paramsService;
  private final IDebitNoteRepo debitNoteRepo;

  @Override
  public void deliverAuthorizedDebitNote(
      DebitNoteDto debitNoteDto, DebitNote debitNote, AuthorizationSri authorizationSri) {
    if (authorizationSri == null
        || !StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
      log.warn(
          "Authorization XML path is missing for debit note {}. Skipping delivery.",
          debitNote != null ? debitNote.getIdDebitNote() : "unknown");
      return;
    }

    boolean emailAlreadySent = Boolean.TRUE.equals(debitNote.getEmailSent());

    try {
      Path storageDir = resolveStorageDir(authorizationSri);
      Path xmlPath = storageDir.resolve(authorizationSri.getAccessKey() + ".xml");
      if (!Files.isRegularFile(xmlPath)) {
        log.warn(
            "Authorization XML file could not be read for debit note {}. Skipping delivery.",
            debitNote != null ? debitNote.getIdDebitNote() : "unknown");
        return;
      }

      byte[] authorizationXmlBytes = Files.readAllBytes(xmlPath);
      String authorizationXml = new String(authorizationXmlBytes, StandardCharsets.UTF_8);

      Path outputDir = storageDir;
      Files.createDirectories(outputDir);

      String logoPath = null;
      String regime = null;
      String specialTaxpayerResolution = null;

      if (debitNoteDto.getCompanyDto() != null) {
        logoPath = debitNoteDto.getCompanyDto().getLogoPath();
        regime = debitNoteDto.getCompanyDto().getRegime();
        specialTaxpayerResolution = debitNoteDto.getCompanyDto().getSpecialTaxpayerResolution();
      }

      log.info(
          "Starting PDF generation for debit note {} with XML path: {}",
          debitNote.getIdDebitNote(),
          xmlPath);
      Path pdfPath =
          debitNotePdfService.generateAndSaveDebitNotePdf(
              authorizationXml, outputDir, logoPath, regime, specialTaxpayerResolution);

      if (pdfPath == null || !Files.exists(pdfPath)) {
        log.error(
            "PDF generation failed for debit note {}. PDF path is null or file does not exist: {}",
            debitNote.getIdDebitNote(),
            pdfPath);
        return;
      }

      byte[] pdfBytes = Files.readAllBytes(pdfPath);

      log.info(
          "PDF generated successfully for debit note {} at path: {} (size: {} bytes)",
          debitNote.getIdDebitNote(),
          pdfPath,
          pdfBytes.length);

      String recipientEmail = getRecipientEmail(debitNoteDto);
      if (StringUtils.hasText(recipientEmail) && !emailAlreadySent) {
        Integer emailResult =
            debitNoteEmailService.sendAuthorizedDebitNote(
                recipientEmail,
                debitNote,
                debitNoteDto,
                authorizationSri,
                pdfBytes,
                authorizationXmlBytes);

        if (emailResult != null && emailResult == 1) {
          debitNote.setEmailSent(true);
          debitNote.setEmailSentDate(LocalDateTime.now());
          debitNoteRepo.save(debitNote);
          log.info(
              "Email sent successfully for debit note {} to recipient {}. Marked as sent.",
              debitNote.getIdDebitNote(),
              recipientEmail);
        } else {
          log.warn(
              "Email sending failed for debit note {} to recipient {}. Will retry on next notification attempt.",
              debitNote.getIdDebitNote(),
              recipientEmail);
        }
      } else if (!StringUtils.hasText(recipientEmail)) {
        log.info(
            "No recipient email found for debit note {}. PDF generated but email not sent.",
            debitNote.getIdDebitNote());
      } else {
        log.info(
            "Email already sent for debit note {} on {}. PDF generated but email skipped.",
            debitNote.getIdDebitNote(),
            debitNote.getEmailSentDate());
      }

    } catch (Exception ex) {
      log.error(
          "Failed to deliver debit note {} to recipient {}",
          debitNote != null ? debitNote.getIdDebitNote() : "unknown",
          debitNoteDto != null ? getRecipientEmail(debitNoteDto) : "unknown",
          ex);
    }
  }

  private String getRecipientEmail(DebitNoteDto debitNoteDto) {
    if (debitNoteDto == null) {
      log.debug("DebitNoteDto is null, cannot get recipient email");
      return null;
    }

    if (debitNoteDto.getClientDto() != null && debitNoteDto.getClientDto().getContacts() != null) {
      String emailFromContacts =
          debitNoteDto.getClientDto().getContacts().stream()
              .filter(contact -> "MAI".equalsIgnoreCase(contact.getContactType()))
              .filter(
                  contact ->
                      contact.getStatus() == null || Boolean.TRUE.equals(contact.getStatus()))
              .findFirst()
              .map(contact -> contact.getValue())
              .orElse(null);

      if (StringUtils.hasText(emailFromContacts)) {
        log.debug(
            "Found recipient email from client contacts for debit note: {}", emailFromContacts);
        return emailFromContacts;
      }
    }

    try {
      java.lang.reflect.Method getBuyerMail = debitNoteDto.getClass().getMethod("getBuyerMail");
      Object buyerMail = getBuyerMail.invoke(debitNoteDto);
      if (buyerMail instanceof String && StringUtils.hasText((String) buyerMail)) {
        log.debug("Found recipient email from buyerMail field for debit note: {}", buyerMail);
        return (String) buyerMail;
      }
    } catch (Exception e) {
      log.trace("buyerMail field not found in DebitNoteDto, trying other methods");
    }

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
      String baseDir = paramsService.getDebitNoteBaseDir();
      return Paths.get(baseDir);
    } catch (Exception ex) {
      throw new IllegalStateException(
          "No se pudo resolver el directorio base de almacenamiento", ex);
    }
  }
}
