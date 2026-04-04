package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebill.utils.dto.AttachmentDto;
import com.wallparisoft.ebill.utils.dto.MailDtoExt;
import com.wallparisoft.ebill.utils.mail.EmailUtil;
import com.wallparisoft.ebillbilling.dto.GuideDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.Guide;
import com.wallparisoft.ebillbilling.service.GuideEmailService;
import com.wallparisoft.ebillbilling.service.ParamsService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuideEmailServiceImpl implements GuideEmailService {

  private static final String PARAM_EMAIL_TEMPLATE = "GUIDE_EMAIL_TEMPLATE";
  private static final String FALLBACK_TEMPLATE_PARAM = "INVOICE_EMAIL_TEMPLATE";
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final ParamsService paramsService;

  @Value("${spring.mail.username}")
  private String mailNotification;

  final EmailUtil emailUtil;

  @Override
  public Integer sendAuthorizedGuide(
      String recipientEmail,
      Guide guide,
      GuideDto guideDto,
      AuthorizationSri authorizationSri,
      byte[] pdfBytes,
      byte[] authorizationXmlBytes) {
    int result = 0;
    if (!StringUtils.hasText(recipientEmail)) {
      log.warn("Skipping guide email. Recipient is empty.");
      return result;
    }

    if (Objects.isNull(guide) || Objects.isNull(authorizationSri) || Objects.isNull(pdfBytes)) {
      log.warn("Skipping guide email. Missing required data.");
      return result;
    }
    if (authorizationXmlBytes == null || authorizationXmlBytes.length == 0) {
      log.warn("Skipping guide email. Authorization XML is empty.");
      return result;
    }
    try {
      String guideNumber = resolveGuideNumber(guide, authorizationSri);
      String html = renderTemplate(guide, guideDto, authorizationSri);

      html = normalizeHtml(html);

      log.debug("Sending guide email with HTML template (length: {} chars)", html.length());
      MailDtoExt mailDto = new MailDtoExt();
      mailDto.setFrom(mailNotification);
      mailDto.setTo(recipientEmail);

      mailDto.setSubject("Guía de remisión electrónica " + guideNumber);
      mailDto.setTemplateHtml(html);
      List<AttachmentDto> listAttachmentDtos = new ArrayList<>();
      listAttachmentDtos.add(new AttachmentDto("GR " + guideNumber + ".pdf", pdfBytes));
      listAttachmentDtos.add(
          new AttachmentDto("GR " + guideNumber + ".xml", authorizationXmlBytes));
      mailDto.setListAttachmentDtos(listAttachmentDtos);
      emailUtil.sendMailWithAttachment(mailDto);
      result = 1;
      return result;
    } catch (IOException e) {
      log.error("Error sending guide email", e);
      return 0;
    }
  }

  private String renderTemplate(Guide guide, GuideDto guideDto, AuthorizationSri authorizationSri)
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
                                "No se pudo cargar la plantilla de email. El parámetro GUIDE_EMAIL_TEMPLATE o INVOICE_EMAIL_TEMPLATE no está configurado en la base de datos.")));

    template = normalizePlaceholders(template);

    String consultationUrl = paramsService.getParamValue("INVOICE_CONSULTATION_URL").orElse("");

    return template
        .replace("{{buyerName}}", resolveBuyerName(guideDto))
        .replace("{{guideNumber}}", resolveGuideNumber(guide, authorizationSri))
        .replace("{{authorizationDate}}", formatDate(authorizationSri.getAuthorizationDate()))
        .replace(
            "{{authorizationNumber}}",
            Optional.ofNullable(authorizationSri.getAuthorizationNumber()).orElse("-"))
        .replace("{{companyName}}", resolveCompanyName(guideDto))
        .replace("{{accessKey}}", Optional.ofNullable(authorizationSri.getAccessKey()).orElse("-"))
        .replace("{{issueDate}}", formatDate(guide.getIssueDate()))
        .replace("{{startDate}}", formatDate(guide.getStartDate()))
        .replace("{{endDate}}", formatDate(guide.getEndDate()))
        .replace("{{carrierName}}", resolveCarrierName(guideDto))
        .replace("{{transportPlate}}", Optional.ofNullable(guide.getTransportPlate()).orElse("-"))
        .replace("{{originAddress}}", Optional.ofNullable(guide.getOriginAddress()).orElse("-"))
        .replace(
            "{{destinationAddress}}",
            Optional.ofNullable(guide.getDestinationAddress()).orElse("-"))
        .replace(
            "{{reasonForTransfer}}", Optional.ofNullable(guide.getReasonForTransfer()).orElse("-"))
        .replace("{{consultationUrl}}", consultationUrl);
  }

  private String normalizePlaceholders(String template) {
    if (template == null || template.isBlank()) {
      return template;
    }

    String[] placeholders = {
      "companyName",
      "buyerName",
      "guideNumber",
      "authorizationDate",
      "authorizationNumber",
      "accessKey",
      "issueDate",
      "startDate",
      "endDate",
      "carrierName",
      "transportPlate",
      "originAddress",
      "destinationAddress",
      "reasonForTransfer",
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

  private String resolveBuyerName(GuideDto guideDto) {
    if (Objects.isNull(guideDto)) {
      return "cliente";
    }
    return Optional.ofNullable(guideDto.getClientDto())
        .map(client -> StringUtils.hasText(client.getName()) ? client.getName() : null)
        .filter(StringUtils::hasText)
        .orElse("cliente");
  }

  private String resolveCompanyName(GuideDto guideDto) {
    if (Objects.isNull(guideDto) || Objects.isNull(guideDto.getCompanyDto())) {
      return "su proveedor";
    }
    return Optional.ofNullable(guideDto.getCompanyDto().getNameCommercial())
        .filter(StringUtils::hasText)
        .orElseGet(
            () ->
                Optional.ofNullable(guideDto.getCompanyDto().getName())
                    .filter(StringUtils::hasText)
                    .orElse("su proveedor"));
  }

  private String resolveCarrierName(GuideDto guideDto) {
    if (Objects.isNull(guideDto) || Objects.isNull(guideDto.getCarrierDto())) {
      return "-";
    }
    return Optional.ofNullable(guideDto.getCarrierDto().getName())
        .filter(StringUtils::hasText)
        .orElse("-");
  }

  private String resolveGuideNumber(Guide guide, AuthorizationSri authorizationSri) {
    return Optional.ofNullable(guide.getGuideNumber())
        .filter(StringUtils::hasText)
        .orElseGet(
            () -> Optional.ofNullable(authorizationSri.getAccessKey()).orElse("Guía de Remisión"));
  }

  private String formatDate(LocalDateTime dateTime) {
    return Optional.ofNullable(dateTime).map(DATE_FORMATTER::format).orElse("-");
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
