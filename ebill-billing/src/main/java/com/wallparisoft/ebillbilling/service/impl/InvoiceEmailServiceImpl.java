package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebill.utils.dto.AttachmentDto;
import com.wallparisoft.ebill.utils.dto.MailDtoExt;
import com.wallparisoft.ebill.utils.mail.EmailUtil;
import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.Invoice;
import com.wallparisoft.ebillbilling.service.InvoiceEmailService;
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
public class InvoiceEmailServiceImpl implements InvoiceEmailService {

  private static final String PARAM_EMAIL_TEMPLATE = "INVOICE_EMAIL_TEMPLATE";
  private static final String PARAM_CONSULTATION_URL = "INVOICE_CONSULTATION_URL";
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

  private final ParamsService paramsService;

  @Value("${spring.mail.username}")
  private String mailNotification;

  final EmailUtil emailUtil;

  @Override
  public Integer sendAuthorizedInvoice(
      String recipientEmail,
      Invoice invoice,
      InvoiceDto invoiceDto,
      AuthorizationSri authorizationSri,
      byte[] pdfBytes,
      byte[] authorizationXmlBytes) {
    int result = 0;
    if (!StringUtils.hasText(recipientEmail)) {
      log.warn("Skipping invoice email. Recipient is empty.");
      return result;
    }

    if (Objects.isNull(invoice) || Objects.isNull(authorizationSri) || Objects.isNull(pdfBytes)) {
      log.warn("Skipping invoice email. Missing required data.");
      return result;
    }
    if (authorizationXmlBytes == null || authorizationXmlBytes.length == 0) {
      log.warn("Skipping invoice email. Authorization XML is empty.");
      return result;
    }
    try {
      String invoiceNumber = resolveInvoiceNumber(invoice, authorizationSri);
      String html;

      html = renderTemplate(invoice, invoiceDto, authorizationSri);

      // Normalizar HTML: eliminar espacios extraños que puedan romper el formato
      html = normalizeHtml(html);

      log.debug("Sending email with HTML template (length: {} chars)", html.length());
      MailDtoExt mailDto = new MailDtoExt();
      mailDto.setFrom(mailNotification);
      mailDto.setTo(recipientEmail);

      mailDto.setSubject("Factura electrónica " + invoiceNumber);
      mailDto.setTemplateHtml(html);
      List<AttachmentDto> listAttachmentDtos = new ArrayList<>();
      listAttachmentDtos.add(new AttachmentDto("FAC " + invoiceNumber + ".pdf", pdfBytes));
      listAttachmentDtos.add(
          new AttachmentDto("FAC " + invoiceNumber + ".xml", authorizationXmlBytes));
      mailDto.setListAttachmentDtos(new ArrayList<>());
      mailDto.setListAttachmentDtos(listAttachmentDtos);
      emailUtil.sendMailWithAttachment(mailDto);
      result = 1;
      return result;
    } catch (IOException e) {
      return 0;
    }
  }

  private String renderTemplate(
      Invoice invoice, InvoiceDto invoiceDto, AuthorizationSri authorizationSri)
      throws IOException {

    // Cargar plantilla desde BD
    String template =
        paramsService
            .getParamValue(PARAM_EMAIL_TEMPLATE)
            .filter(t -> !t.isBlank())
            .orElseThrow(
                () ->
                    new IOException(
                        "No se pudo cargar la plantilla de email. El parámetro INVOICE_EMAIL_TEMPLATE no está configurado en la base de datos."));

    // Normalizar el template primero para corregir espacios en placeholders
    template = normalizePlaceholders(template);

    String consultationUrl = paramsService.getParamValueOrDefault(PARAM_CONSULTATION_URL, "");

    // Reemplazar placeholders con valores reales
    return template
        .replace("{{buyerName}}", resolveBuyerName(invoiceDto))
        .replace("{{invoiceNumber}}", resolveInvoiceNumber(invoice, authorizationSri))
        .replace("{{authorizationDate}}", formatDate(authorizationSri.getAuthorizationDate()))
        .replace(
            "{{authorizationNumber}}",
            Optional.ofNullable(authorizationSri.getAuthorizationNumber()).orElse("-"))
        .replace("{{totalAmount}}", formatMoney(invoice.getTotalAmount()))
        .replace("{{companyName}}", resolveCompanyName(invoiceDto))
        .replace("{{accessKey}}", Optional.ofNullable(authorizationSri.getAccessKey()).orElse("-"))
        .replace("{{issueDate}}", formatDate(invoice.getCreationDate()))
        .replace("{{consultationUrl}}", consultationUrl);
  }

  /**
   * Normaliza los placeholders en el template, corrigiendo espacios extraños. Por ejemplo: { {
   * companyName } } -> {{companyName}}
   *
   * @param template Template HTML con placeholders
   * @return Template con placeholders normalizados
   */
  private String normalizePlaceholders(String template) {
    if (template == null || template.isBlank()) {
      return template;
    }

    // Lista de placeholders conocidos
    String[] placeholders = {
      "companyName",
      "buyerName",
      "invoiceNumber",
      "authorizationDate",
      "authorizationNumber",
      "accessKey",
      "issueDate",
      "totalAmount",
      "consultationUrl"
    };

    String normalized = template;
    for (String placeholder : placeholders) {
      // Corregir espacios en placeholders: { { placeholder } } -> {{placeholder}}
      normalized =
          normalized.replaceAll(
              "\\{\\s*\\{\\s*" + placeholder + "\\s*\\}\\s*\\}", "{{" + placeholder + "}}");
    }

    return normalized;
  }

  private String resolveBuyerName(InvoiceDto invoiceDto) {
    if (Objects.isNull(invoiceDto)) {
      return "cliente";
    }
    return Optional.ofNullable(invoiceDto.getBuyerName())
        .filter(StringUtils::hasText)
        .or(
            () ->
                Optional.ofNullable(invoiceDto.getClientDto())
                    .map(client -> StringUtils.hasText(client.getName()) ? client.getName() : null))
        .filter(StringUtils::hasText)
        .orElse("cliente");
  }

  private String resolveCompanyName(InvoiceDto invoiceDto) {
    if (Objects.isNull(invoiceDto) || Objects.isNull(invoiceDto.getCompanyDto())) {
      return "su proveedor";
    }
    return Optional.ofNullable(invoiceDto.getCompanyDto().getNameCommercial())
        .filter(StringUtils::hasText)
        .orElseGet(
            () ->
                Optional.ofNullable(invoiceDto.getCompanyDto().getName())
                    .filter(StringUtils::hasText)
                    .orElse("su proveedor"));
  }

  private String resolveInvoiceNumber(Invoice invoice, AuthorizationSri authorizationSri) {
    return Optional.ofNullable(invoice.getInvoiceNumber())
        .filter(StringUtils::hasText)
        .orElseGet(() -> Optional.ofNullable(authorizationSri.getAccessKey()).orElse("Factura"));
  }

  private String resolveCreateDate(InvoiceDto invoiceDto) {
    return Optional.ofNullable(invoiceDto)
        .map(InvoiceDto::getCreationDate)
        .map(LocalDateTime::toString)
        .orElse("-");
  }

  private String formatDate(LocalDateTime dateTime) {
    return Optional.ofNullable(dateTime).map(DATE_FORMATTER::format).orElse("-");
  }

  private String formatMoney(@NonNull java.math.BigDecimal value) {
    return Optional.ofNullable(value).map(MONEY_FORMAT::format).orElse("0.00");
  }

  /**
   * Normaliza el HTML eliminando espacios extraños que puedan romper el formato del correo. Corrige
   * problemas comunes como espacios entre propiedades CSS y placeholders.
   *
   * @param html HTML a normalizar
   * @return HTML normalizado
   */
  private String normalizeHtml(String html) {
    if (html == null || html.isBlank()) {
      return html;
    }

    String normalized = html;

    // Corregir espacios en DOCTYPE
    normalized = normalized.replaceAll("<\\s*!\\s*DOCTYPE", "<!DOCTYPE");

    // Corregir espacios en placeholders {{variable}} - importante hacerlo primero
    normalized = normalized.replaceAll("\\{\\s*\\{\\s*", "{{");
    normalized = normalized.replaceAll("\\s*\\}\\s*\\}", "}}");

    // Corregir espacios en propiedades CSS comunes
    normalized = normalized.replaceAll("font\\s*-\\s*family", "font-family");
    normalized = normalized.replaceAll("background\\s*-\\s*color", "background-color");
    normalized = normalized.replaceAll("font\\s*-\\s*size", "font-size");
    normalized = normalized.replaceAll("line\\s*-\\s*height", "line-height");
    normalized = normalized.replaceAll("border\\s*-\\s*radius", "border-radius");
    normalized = normalized.replaceAll("border\\s*-\\s*collapse", "border-collapse");
    normalized = normalized.replaceAll("border\\s*-\\s*top", "border-top");
    normalized = normalized.replaceAll("box\\s*-\\s*sizing", "box-sizing");
    normalized = normalized.replaceAll("box\\s*-\\s*shadow", "box-shadow");
    normalized = normalized.replaceAll("text\\s*-\\s*align", "text-align");
    normalized = normalized.replaceAll("text\\s*-\\s*decoration", "text-decoration");
    normalized = normalized.replaceAll("text\\s*-\\s*transform", "text-transform");
    normalized = normalized.replaceAll("letter\\s*-\\s*spacing", "letter-spacing");
    normalized = normalized.replaceAll("font\\s*-\\s*weight", "font-weight");
    normalized = normalized.replaceAll("margin\\s*-\\s*top", "margin-top");
    normalized = normalized.replaceAll("margin\\s*-\\s*bottom", "margin-bottom");
    normalized = normalized.replaceAll("padding\\s*-\\s*left", "padding-left");
    normalized = normalized.replaceAll("max\\s*-\\s*width", "max-width");
    normalized = normalized.replaceAll("vertical\\s*-\\s*align", "vertical-align");
    normalized =
        normalized.replaceAll("display\\s*:\\s*inline\\s*-\\s*flex", "display: inline-flex");
    normalized = normalized.replaceAll("align\\s*-\\s*items", "align-items");
    normalized = normalized.replaceAll("justify\\s*-\\s*content", "justify-content");
    normalized = normalized.replaceAll("header\\s*-\\s*brand", "header-brand");
    normalized = normalized.replaceAll("info\\s*-\\s*icon", "info-icon");
    normalized = normalized.replaceAll("footer\\s*-\\s*bar", "footer-bar");

    // Corregir espacios en porcentajes dentro de CSS
    normalized = normalized.replaceAll("(\\d+)\\s*%", "$1%");

    // Corregir espacios en !important
    normalized = normalized.replaceAll("!\\s*important", "!important");

    // Corregir espacios en etiquetas HTML comunes (solo al inicio)
    normalized = normalized.replaceAll("<\\s*html\\s", "<html ");
    normalized = normalized.replaceAll("<\\s*head\\s", "<head ");
    normalized = normalized.replaceAll("<\\s*body\\s", "<body ");
    normalized = normalized.replaceAll("<\\s*meta\\s", "<meta ");
    normalized = normalized.replaceAll("<\\s*title\\s", "<title ");
    normalized = normalized.replaceAll("<\\s*style\\s", "<style ");
    normalized = normalized.replaceAll("<\\s*table\\s", "<table ");
    normalized = normalized.replaceAll("<\\s*tr\\s", "<tr ");
    normalized = normalized.replaceAll("<\\s*td\\s", "<td ");
    normalized = normalized.replaceAll("<\\s*div\\s", "<div ");
    normalized = normalized.replaceAll("<\\s*h1\\s", "<h1 ");
    normalized = normalized.replaceAll("<\\s*h2\\s", "<h2 ");
    normalized = normalized.replaceAll("<\\s*p\\s", "<p ");
    normalized = normalized.replaceAll("<\\s*ul\\s", "<ul ");
    normalized = normalized.replaceAll("<\\s*li\\s", "<li ");
    normalized = normalized.replaceAll("<\\s*a\\s", "<a ");
    normalized = normalized.replaceAll("<\\s*br\\s*/\\s*>", "<br/>");

    // Corregir espacios en etiquetas de cierre
    normalized = normalized.replaceAll("<\\s*/\\s*html\\s*>", "</html>");
    normalized = normalized.replaceAll("<\\s*/\\s*head\\s*>", "</head>");
    normalized = normalized.replaceAll("<\\s*/\\s*body\\s*>", "</body>");
    normalized = normalized.replaceAll("<\\s*/\\s*meta\\s*>", "</meta>");
    normalized = normalized.replaceAll("<\\s*/\\s*title\\s*>", "</title>");
    normalized = normalized.replaceAll("<\\s*/\\s*style\\s*>", "</style>");
    normalized = normalized.replaceAll("<\\s*/\\s*table\\s*>", "</table>");
    normalized = normalized.replaceAll("<\\s*/\\s*tr\\s*>", "</tr>");
    normalized = normalized.replaceAll("<\\s*/\\s*td\\s*>", "</td>");
    normalized = normalized.replaceAll("<\\s*/\\s*div\\s*>", "</div>");
    normalized = normalized.replaceAll("<\\s*/\\s*h1\\s*>", "</h1>");
    normalized = normalized.replaceAll("<\\s*/\\s*h2\\s*>", "</h2>");
    normalized = normalized.replaceAll("<\\s*/\\s*p\\s*>", "</p>");
    normalized = normalized.replaceAll("<\\s*/\\s*ul\\s*>", "</ul>");
    normalized = normalized.replaceAll("<\\s*/\\s*li\\s*>", "</li>");
    normalized = normalized.replaceAll("<\\s*/\\s*a\\s*>", "</a>");

    // Corregir espacios en atributos comunes
    normalized = normalized.replaceAll("lang\\s*=", "lang=");
    normalized = normalized.replaceAll("xmlns\\s*=", "xmlns=");
    normalized = normalized.replaceAll("charset\\s*=", "charset=");
    normalized = normalized.replaceAll("name\\s*=", "name=");
    normalized = normalized.replaceAll("content\\s*=", "content=");
    normalized = normalized.replaceAll("role\\s*=", "role=");
    normalized = normalized.replaceAll("border\\s*=", "border=");
    normalized = normalized.replaceAll("cellpadding\\s*=", "cellpadding=");
    normalized = normalized.replaceAll("cellspacing\\s*=", "cellspacing=");
    normalized = normalized.replaceAll("class\\s*=", "class=");
    normalized = normalized.replaceAll("href\\s*=", "href=");
    normalized = normalized.replaceAll("style\\s*=", "style=");

    return normalized;
  }
}
