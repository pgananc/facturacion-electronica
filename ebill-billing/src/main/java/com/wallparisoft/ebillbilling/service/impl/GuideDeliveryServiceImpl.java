package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.GuideDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.Guide;
import com.wallparisoft.ebillbilling.repository.IGuideRepo;
import com.wallparisoft.ebillbilling.service.GuideDeliveryService;
import com.wallparisoft.ebillbilling.service.GuideEmailService;
import com.wallparisoft.ebillbilling.service.GuidePdfService;
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
public class GuideDeliveryServiceImpl implements GuideDeliveryService {

  private final GuidePdfService guidePdfService;
  private final GuideEmailService guideEmailService;
  private final ParamsService paramsService;
  private final IGuideRepo guideRepo;

  @Override
  public void deliverAuthorizedGuide(
      GuideDto guideDto, Guide guide, AuthorizationSri authorizationSri) {
    if (authorizationSri == null
        || !StringUtils.hasText(authorizationSri.getAuthorizationStoragePath())) {
      log.warn(
          "Authorization XML path is missing for guide {}. Skipping delivery.",
          guide != null ? guide.getIdGuide() : "unknown");
      return;
    }

    boolean emailAlreadySent = Boolean.TRUE.equals(guide.getEmailSent());

    try {
      Path storageDir = resolveStorageDir(authorizationSri);
      Path xmlPath = storageDir.resolve(authorizationSri.getAccessKey() + ".xml");
      if (!Files.isRegularFile(xmlPath)) {
        log.warn(
            "Authorization XML file could not be read for guide {}. Skipping delivery.",
            guide != null ? guide.getIdGuide() : "unknown");
        return;
      }

      byte[] authorizationXmlBytes = Files.readAllBytes(xmlPath);
      String authorizationXml = new String(authorizationXmlBytes, StandardCharsets.UTF_8);

      Path outputDir = storageDir;
      Files.createDirectories(outputDir);

      String logoPath = null;
      String regime = null;
      String specialTaxpayerResolution = null;

      if (guideDto.getCompanyDto() != null) {
        logoPath = guideDto.getCompanyDto().getLogoPath();
        regime = guideDto.getCompanyDto().getRegime();
        specialTaxpayerResolution = guideDto.getCompanyDto().getSpecialTaxpayerResolution();
      }

      log.info(
          "Starting PDF generation for guide {} with XML path: {}", guide.getIdGuide(), xmlPath);
      Path pdfPath =
          guidePdfService.generateAndSaveGuidePdf(
              authorizationXml, outputDir, logoPath, regime, specialTaxpayerResolution);

      if (pdfPath == null || !Files.exists(pdfPath)) {
        log.error(
            "PDF generation failed for guide {}. PDF path is null or file does not exist: {}",
            guide.getIdGuide(),
            pdfPath);
        return;
      }

      byte[] pdfBytes = Files.readAllBytes(pdfPath);

      log.info(
          "PDF generated successfully for guide {} at path: {} (size: {} bytes)",
          guide.getIdGuide(),
          pdfPath,
          pdfBytes.length);

      String recipientEmail = getRecipientEmail(guideDto);
      if (StringUtils.hasText(recipientEmail) && !emailAlreadySent) {
        Integer emailResult =
            guideEmailService.sendAuthorizedGuide(
                recipientEmail, guide, guideDto, authorizationSri, pdfBytes, authorizationXmlBytes);

        if (emailResult != null && emailResult == 1) {
          guide.setEmailSent(true);
          guide.setEmailSentDate(LocalDateTime.now());
          guideRepo.save(guide);
          log.info(
              "Email sent successfully for guide {} to recipient {}. Marked as sent.",
              guide.getIdGuide(),
              recipientEmail);
        } else {
          log.warn(
              "Email sending failed for guide {} to recipient {}. Will retry on next notification attempt.",
              guide.getIdGuide(),
              recipientEmail);
        }
      } else if (!StringUtils.hasText(recipientEmail)) {
        log.info(
            "No recipient email found for guide {}. PDF generated but email not sent.",
            guide.getIdGuide());
      } else {
        log.info(
            "Email already sent for guide {} on {}. PDF generated but email skipped.",
            guide.getIdGuide(),
            guide.getEmailSentDate());
      }

    } catch (Exception ex) {
      log.error(
          "Failed to deliver guide {} to recipient {}",
          guide != null ? guide.getIdGuide() : "unknown",
          guideDto != null ? getRecipientEmail(guideDto) : "unknown",
          ex);
    }
  }

  private String getRecipientEmail(GuideDto guideDto) {
    if (guideDto == null) {
      log.debug("GuideDto is null, cannot get recipient email");
      return null;
    }

    if (guideDto.getClientDto() != null && guideDto.getClientDto().getContacts() != null) {
      String emailFromContacts =
          guideDto.getClientDto().getContacts().stream()
              .filter(contact -> "MAI".equalsIgnoreCase(contact.getContactType()))
              .filter(
                  contact ->
                      contact.getStatus() == null || Boolean.TRUE.equals(contact.getStatus()))
              .findFirst()
              .map(contact -> contact.getValue())
              .orElse(null);

      if (StringUtils.hasText(emailFromContacts)) {
        log.debug("Found recipient email from client contacts for guide: {}", emailFromContacts);
        return emailFromContacts;
      }
    }

    try {
      java.lang.reflect.Method getBuyerMail = guideDto.getClass().getMethod("getBuyerMail");
      Object buyerMail = getBuyerMail.invoke(guideDto);
      if (buyerMail instanceof String && StringUtils.hasText((String) buyerMail)) {
        log.debug("Found recipient email from buyerMail field for guide: {}", buyerMail);
        return (String) buyerMail;
      }
    } catch (Exception e) {
      log.trace("buyerMail field not found in GuideDto, trying other methods");
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
      String baseDir = paramsService.getGuideBaseDir();
      return Paths.get(baseDir);
    } catch (Exception ex) {
      throw new IllegalStateException(
          "No se pudo resolver el directorio base de almacenamiento", ex);
    }
  }
}
