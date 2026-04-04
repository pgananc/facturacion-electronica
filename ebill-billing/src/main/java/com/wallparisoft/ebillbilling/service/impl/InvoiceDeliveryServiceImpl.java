package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.Invoice;
import com.wallparisoft.ebillbilling.repository.IInvoiceRepo;
import com.wallparisoft.ebillbilling.service.InvoiceDeliveryService;
import com.wallparisoft.ebillbilling.service.InvoiceEmailService;
import com.wallparisoft.ebillbilling.service.InvoicePdfService;
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
public class InvoiceDeliveryServiceImpl implements InvoiceDeliveryService {

  private final InvoicePdfService invoicePdfService;
  private final InvoiceEmailService invoiceEmailService;
  private final ParamsService paramsService;
  private final IInvoiceRepo invoiceRepo;

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
          "PDF generated successfully for invoice {} at path: {}", invoice.getIdInvoice(), pdfPath);

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
      String baseDir = paramsService.getBaseDir();
      return Paths.get(baseDir);
    } catch (Exception ex) {
      throw new IllegalStateException(
          "No se pudo resolver el directorio base de almacenamiento", ex);
    }
  }
}
