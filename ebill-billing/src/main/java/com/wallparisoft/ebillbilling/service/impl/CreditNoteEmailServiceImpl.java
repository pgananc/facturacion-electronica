package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebill.utils.dto.AttachmentDto;
import com.wallparisoft.ebill.utils.dto.MailDtoExt;
import com.wallparisoft.ebill.utils.mail.EmailUtil;
import com.wallparisoft.ebillbilling.dto.CreditNoteDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.CreditNote;
import com.wallparisoft.ebillbilling.service.CreditNoteEmailService;
import com.wallparisoft.ebillbilling.service.ParamsService;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditNoteEmailServiceImpl implements CreditNoteEmailService {

  private static final String PARAM_EMAIL_TEMPLATE = "CREDIT_NOTE_EMAIL_TEMPLATE";
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

  private final ParamsService paramsService;

  @Value("${spring.mail.username}")
  private String mailNotification;

  final EmailUtil emailUtil;

  @Override
  public Integer sendAuthorizedCreditNote(
      String recipientEmail,
      CreditNote creditNote,
      CreditNoteDto creditNoteDto,
      AuthorizationSri authorizationSri,
      byte[] pdfBytes,
      byte[] authorizationXmlBytes) {
    int result = 0;
    if (!StringUtils.hasText(recipientEmail)) {
      log.warn("Skipping credit note email. Recipient is empty.");
      return result;
    }

    if (Objects.isNull(creditNote)
        || Objects.isNull(authorizationSri)
        || Objects.isNull(pdfBytes)) {
      log.warn("Skipping credit note email. Missing required data.");
      return result;
    }
    if (authorizationXmlBytes == null || authorizationXmlBytes.length == 0) {
      log.warn("Skipping credit note email. Authorization XML is empty.");
      return result;
    }
    try {
      String creditNoteNumber = resolveCreditNoteNumber(creditNote, authorizationSri);
      String html = renderTemplate(creditNote, creditNoteDto, authorizationSri);

      // Normalizar HTML
      html = normalizeHtml(html);

      log.debug("Sending credit note email with HTML template (length: {} chars)", html.length());
      MailDtoExt mailDto = new MailDtoExt();
      mailDto.setFrom(mailNotification);
      mailDto.setTo(recipientEmail);

      mailDto.setSubject("Nota de crédito electrónica " + creditNoteNumber);
      mailDto.setTemplateHtml(html);
      List<AttachmentDto> listAttachmentDtos = new ArrayList<>();
      listAttachmentDtos.add(new AttachmentDto("NC " + creditNoteNumber + ".pdf", pdfBytes));
      listAttachmentDtos.add(
          new AttachmentDto("NC " + creditNoteNumber + ".xml", authorizationXmlBytes));
      mailDto.setListAttachmentDtos(listAttachmentDtos);
      emailUtil.sendMailWithAttachment(mailDto);
      result = 1;
      return result;
    } catch (IOException e) {
      log.error("Error sending credit note email", e);
      return 0;
    }
  }

  private String renderTemplate(
      CreditNote creditNote, CreditNoteDto creditNoteDto, AuthorizationSri authorizationSri)
      throws IOException {

    // Cargar plantilla desde BD, con fallback a plantilla de factura si no existe
    String template =
        paramsService
            .getParamValue(PARAM_EMAIL_TEMPLATE)
            .orElseThrow(
                () ->
                    new IOException(
                        "No se pudo cargar la plantilla de email. El parámetro CREDIT_NOTE_EMAIL_TEMPLATE o INVOICE_EMAIL_TEMPLATE no está configurado en la base de datos."));

    template = normalizePlaceholders(template);

    String consultationUrl = paramsService.getParamValue("INVOICE_CONSULTATION_URL").orElse("");

    // Reemplazar placeholders con valores reales
    return template
        .replace("{{buyerName}}", resolveBuyerName(creditNoteDto))
        .replace("{{creditNoteNumber}}", resolveCreditNoteNumber(creditNote, authorizationSri))
        .replace("{{authorizationDate}}", formatDate(authorizationSri.getAuthorizationDate()))
        .replace(
            "{{authorizationNumber}}",
            Optional.ofNullable(authorizationSri.getAuthorizationNumber()).orElse("-"))
        .replace("{{totalAmount}}", formatMoney(creditNote.getTotalAmount()))
        .replace("{{valorModificacion}}", formatMoney(creditNote.getValorModificacion()))
        .replace("{{companyName}}", resolveCompanyName(creditNoteDto))
        .replace("{{accessKey}}", Optional.ofNullable(authorizationSri.getAccessKey()).orElse("-"))
        .replace("{{issueDate}}", formatDate(creditNote.getCreationDate()))
        .replace("{{reason}}", Optional.ofNullable(creditNote.getReason()).orElse("-"))
        .replace("{{consultationUrl}}", consultationUrl);
  }

  private String normalizePlaceholders(String template) {
    if (template == null || template.isBlank()) {
      return template;
    }

    String[] placeholders = {
      "companyName",
      "buyerName",
      "creditNoteNumber",
      "invoiceNumber",
      "authorizationDate",
      "authorizationNumber",
      "accessKey",
      "issueDate",
      "totalAmount",
      "valorModificacion",
      "reason",
      "consultationUrl"
    };

    String normalized = template;
    for (String placeholder : placeholders) {
      normalized =
          normalized.replaceAll(
              "\\{\\s*\\{\\s*" + placeholder + "\\s*\\}\\s*\\}", "{{" + placeholder + "}}");
    }

    return normalized;
  }

  private String resolveBuyerName(CreditNoteDto creditNoteDto) {
    if (Objects.isNull(creditNoteDto)) {
      return "cliente";
    }
    return Optional.ofNullable(creditNoteDto.getClientDto())
        .map(client -> StringUtils.hasText(client.getName()) ? client.getName() : null)
        .filter(StringUtils::hasText)
        .orElse("cliente");
  }

  private String resolveCompanyName(CreditNoteDto creditNoteDto) {
    if (Objects.isNull(creditNoteDto) || Objects.isNull(creditNoteDto.getCompanyDto())) {
      return "su proveedor";
    }
    return Optional.ofNullable(creditNoteDto.getCompanyDto().getNameCommercial())
        .filter(StringUtils::hasText)
        .orElseGet(
            () ->
                Optional.ofNullable(creditNoteDto.getCompanyDto().getName())
                    .filter(StringUtils::hasText)
                    .orElse("su proveedor"));
  }

  private String resolveCreditNoteNumber(CreditNote creditNote, AuthorizationSri authorizationSri) {
    return Optional.ofNullable(creditNote.getCreditNoteNumber())
        .filter(StringUtils::hasText)
        .orElseGet(
            () -> Optional.ofNullable(authorizationSri.getAccessKey()).orElse("Nota de Crédito"));
  }

  private String formatDate(LocalDateTime dateTime) {
    return Optional.ofNullable(dateTime).map(DATE_FORMATTER::format).orElse("-");
  }

  private String formatMoney(@NonNull java.math.BigDecimal value) {
    return Optional.ofNullable(value).map(MONEY_FORMAT::format).orElse("0.00");
  }

  private String normalizeHtml(String html) {
    if (html == null || html.isBlank()) {
      return html;
    }

    // Reutilizar la misma lógica de normalización que InvoiceEmailServiceImpl
    String normalized = html;
    normalized = normalized.replaceAll("\\{\\s*\\{\\s*", "{{");
    normalized = normalized.replaceAll("\\s*\\}\\s*\\}", "}}");
    normalized = normalized.replaceAll("font\\s*-\\s*family", "font-family");
    normalized = normalized.replaceAll("background\\s*-\\s*color", "background-color");
    normalized = normalized.replaceAll("text\\s*-\\s*align", "text-align");

    return normalized;
  }
}
