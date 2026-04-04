package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebill.utils.dto.AttachmentDto;
import com.wallparisoft.ebill.utils.dto.MailDtoExt;
import com.wallparisoft.ebill.utils.mail.EmailUtil;
import com.wallparisoft.ebillbilling.dto.DebitNoteDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.DebitNote;
import com.wallparisoft.ebillbilling.service.DebitNoteEmailService;
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
public class DebitNoteEmailServiceImpl implements DebitNoteEmailService {

  private static final String PARAM_EMAIL_TEMPLATE = "DEBIT_NOTE_EMAIL_TEMPLATE";
  private static final String FALLBACK_TEMPLATE_PARAM = "INVOICE_EMAIL_TEMPLATE";
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

  private final ParamsService paramsService;

  @Value("${spring.mail.username}")
  private String mailNotification;

  final EmailUtil emailUtil;

  @Override
  public Integer sendAuthorizedDebitNote(
      String recipientEmail,
      DebitNote debitNote,
      DebitNoteDto debitNoteDto,
      AuthorizationSri authorizationSri,
      byte[] pdfBytes,
      byte[] authorizationXmlBytes) {
    int result = 0;
    if (!StringUtils.hasText(recipientEmail)) {
      log.warn("Skipping debit note email. Recipient is empty.");
      return result;
    }

    if (Objects.isNull(debitNote) || Objects.isNull(authorizationSri) || Objects.isNull(pdfBytes)) {
      log.warn("Skipping debit note email. Missing required data.");
      return result;
    }
    if (authorizationXmlBytes == null || authorizationXmlBytes.length == 0) {
      log.warn("Skipping debit note email. Authorization XML is empty.");
      return result;
    }
    try {
      String debitNoteNumber = resolveDebitNoteNumber(debitNote, authorizationSri);
      String html = renderTemplate(debitNote, debitNoteDto, authorizationSri);

      html = normalizeHtml(html);

      log.debug("Sending debit note email with HTML template (length: {} chars)", html.length());
      MailDtoExt mailDto = new MailDtoExt();
      mailDto.setFrom(mailNotification);
      mailDto.setTo(recipientEmail);

      mailDto.setSubject("Nota de débito electrónica " + debitNoteNumber);
      mailDto.setTemplateHtml(html);
      List<AttachmentDto> listAttachmentDtos = new ArrayList<>();
      listAttachmentDtos.add(new AttachmentDto("ND " + debitNoteNumber + ".pdf", pdfBytes));
      listAttachmentDtos.add(
          new AttachmentDto("ND " + debitNoteNumber + ".xml", authorizationXmlBytes));
      mailDto.setListAttachmentDtos(listAttachmentDtos);
      emailUtil.sendMailWithAttachment(mailDto);
      result = 1;
      return result;
    } catch (IOException e) {
      log.error("Error sending debit note email", e);
      return 0;
    }
  }

  private String renderTemplate(
      DebitNote debitNote, DebitNoteDto debitNoteDto, AuthorizationSri authorizationSri)
      throws IOException {

    String template =
        paramsService
            .getParamValue(PARAM_EMAIL_TEMPLATE)
            .orElse(
                paramsService
                    .getParamValue(FALLBACK_TEMPLATE_PARAM)
                    .orElseThrow(
                        () ->
                            new IOException(
                                "No se pudo cargar la plantilla de email. El parámetro DEBIT_NOTE_EMAIL_TEMPLATE o INVOICE_EMAIL_TEMPLATE no está configurado en la base de datos.")));

    template = normalizePlaceholders(template);

    String consultationUrl = paramsService.getParamValue("INVOICE_CONSULTATION_URL").orElse("");

    return template
        .replace("{{buyerName}}", resolveBuyerName(debitNoteDto))
        .replace("{{debitNoteNumber}}", resolveDebitNoteNumber(debitNote, authorizationSri))
        .replace("{{authorizationDate}}", formatDate(authorizationSri.getAuthorizationDate()))
        .replace(
            "{{authorizationNumber}}",
            Optional.ofNullable(authorizationSri.getAuthorizationNumber()).orElse("-"))
        .replace("{{totalAmount}}", formatMoney(debitNote.getTotalAmount()))
        .replace("{{valorModificacion}}", formatMoney(debitNote.getValorModificacion()))
        .replace("{{companyName}}", resolveCompanyName(debitNoteDto))
        .replace("{{accessKey}}", Optional.ofNullable(authorizationSri.getAccessKey()).orElse("-"))
        .replace("{{issueDate}}", formatDate(debitNote.getCreationDate()))
        .replace("{{reason}}", formatMotivos(debitNoteDto))
        .replace("{{consultationUrl}}", consultationUrl);
  }

  private String normalizePlaceholders(String template) {
    if (template == null || template.isBlank()) {
      return template;
    }

    String[] placeholders = {
      "companyName",
      "buyerName",
      "debitNoteNumber",
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

  private String resolveBuyerName(DebitNoteDto debitNoteDto) {
    if (Objects.isNull(debitNoteDto)) {
      return "cliente";
    }
    return Optional.ofNullable(debitNoteDto.getClientDto())
        .map(client -> StringUtils.hasText(client.getName()) ? client.getName() : null)
        .filter(StringUtils::hasText)
        .orElse("cliente");
  }

  private String resolveCompanyName(DebitNoteDto debitNoteDto) {
    if (Objects.isNull(debitNoteDto) || Objects.isNull(debitNoteDto.getCompanyDto())) {
      return "su proveedor";
    }
    return Optional.ofNullable(debitNoteDto.getCompanyDto().getNameCommercial())
        .filter(StringUtils::hasText)
        .orElseGet(
            () ->
                Optional.ofNullable(debitNoteDto.getCompanyDto().getName())
                    .filter(StringUtils::hasText)
                    .orElse("su proveedor"));
  }

  private String resolveDebitNoteNumber(DebitNote debitNote, AuthorizationSri authorizationSri) {
    return Optional.ofNullable(debitNote.getDebitNoteNumber())
        .filter(StringUtils::hasText)
        .orElseGet(
            () -> Optional.ofNullable(authorizationSri.getAccessKey()).orElse("Nota de Débito"));
  }

  private String formatDate(LocalDateTime dateTime) {
    return Optional.ofNullable(dateTime).map(DATE_FORMATTER::format).orElse("-");
  }

  private String formatMoney(@NonNull java.math.BigDecimal value) {
    return Optional.ofNullable(value).map(MONEY_FORMAT::format).orElse("0.00");
  }

  private String formatMotivos(DebitNoteDto debitNoteDto) {
    if (debitNoteDto == null
        || debitNoteDto.getDebitNoteDetails() == null
        || debitNoteDto.getDebitNoteDetails().isEmpty()) {
      return "-";
    }
    return debitNoteDto.getDebitNoteDetails().stream()
        .map(
            detail -> {
              String razon = "-";
              if (detail.getProductDto() != null) {
                razon =
                    StringUtils.hasText(detail.getProductDto().getName())
                        ? detail.getProductDto().getName()
                        : (StringUtils.hasText(detail.getProductDto().getDescription())
                            ? detail.getProductDto().getDescription()
                            : "-");
              }
              String valor =
                  Optional.ofNullable(detail.getSubtotal())
                      .map(MONEY_FORMAT::format)
                      .orElse("0.00");
              return razon + " ($" + valor + ")";
            })
        .reduce((a, b) -> a + "<br/>" + b)
        .orElse("-");
  }

  private String normalizeHtml(String html) {
    if (html == null || html.isBlank()) {
      return html;
    }

    String normalized = html;
    normalized = normalized.replaceAll("\\{\\s*\\{\\s*", "{{");
    normalized = normalized.replaceAll("\\s*\\}\\s*\\}", "}}");
    normalized = normalized.replaceAll("font\\s*-\\s*family", "font-family");
    normalized = normalized.replaceAll("background\\s*-\\s*color", "background-color");
    normalized = normalized.replaceAll("text\\s*-\\s*align", "text-align");

    return normalized;
  }
}
