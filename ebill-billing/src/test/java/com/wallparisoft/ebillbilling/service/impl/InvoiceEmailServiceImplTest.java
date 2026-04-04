package com.wallparisoft.ebillbilling.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.wallparisoft.ebill.utils.dto.AttachmentDto;
import com.wallparisoft.ebill.utils.dto.MailDtoExt;
import com.wallparisoft.ebill.utils.mail.EmailUtil;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.Invoice;
import com.wallparisoft.ebillbilling.service.ParamsService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class InvoiceEmailServiceImplTest {

  private EmailUtil emailUtil;
  private ParamsService paramsService;
  private InvoiceEmailServiceImpl invoiceEmailService;

  @BeforeEach
  void setUp() {
    emailUtil = Mockito.mock(EmailUtil.class);
    paramsService = Mockito.mock(ParamsService.class);

    // Mock para que getParamValueOrDefault retorne valores por defecto
    Mockito.when(
            paramsService.getParamValueOrDefault(
                Mockito.eq("INVOICE_CONSULTATION_URL"), Mockito.anyString()))
        .thenReturn("http://localhost:4200/invoice-consultation");

    // Mock del template HTML desde BD
    String htmlTemplate =
        "<html><body><h1>{{companyName}}</h1><p>Estimado/a <strong>{{buyerName}}</strong></p><p>Factura: {{invoiceNumber}}</p><p>Fecha: {{authorizationDate}}</p><p>Total: ${{totalAmount}}</p><a href=\"{{consultationUrl}}\">aquí</a></body></html>";
    Mockito.when(paramsService.getParamValue(Mockito.eq("INVOICE_EMAIL_TEMPLATE")))
        .thenReturn(Optional.of(htmlTemplate));

    invoiceEmailService = new InvoiceEmailServiceImpl(paramsService, emailUtil);
    ReflectionTestUtils.setField(invoiceEmailService, "mailNotification", "billing@test.com");
  }

  @Test
  void shouldSendEmailWithAttachmentsWhenDataIsValid() {
    Invoice invoice = new Invoice();
    invoice.setInvoiceNumber("001-002-000000123");
    invoice.setTotalAmount(new BigDecimal("250.45"));

    AuthorizationSri authorizationSri = new AuthorizationSri();
    authorizationSri.setAuthorizationStatusSri("AUTORIZADO");
    authorizationSri.setAuthorizationDate(LocalDateTime.of(2025, 11, 8, 10, 30));
    authorizationSri.setAuthorizationNumber("1234567890");
    authorizationSri.setAccessKey("081120251234567890");

    InvoiceDto invoiceDto = new InvoiceDto();
    invoiceDto.setBuyerMail("customer@example.com");
    invoiceDto.setBuyerName("Cliente Demo");
    invoiceDto.setCompanyDto(CompanyDto.builder().nameCommercial("Demo Corp").build());

    byte[] pdfBytes = "PDF".getBytes(StandardCharsets.UTF_8);
    byte[] authorizationXmlBytes = "<xml>authorized</xml>".getBytes(StandardCharsets.UTF_8);

    Integer result =
        invoiceEmailService.sendAuthorizedInvoice(
            invoiceDto.getBuyerMail(),
            invoice,
            invoiceDto,
            authorizationSri,
            pdfBytes,
            authorizationXmlBytes);

    assertEquals(1, result);

    ArgumentCaptor<MailDtoExt> mailCaptor = ArgumentCaptor.forClass(MailDtoExt.class);
    verify(emailUtil).sendMailWithAttachment(mailCaptor.capture());

    MailDtoExt mailDto = mailCaptor.getValue();
    assertEquals("billing@test.com", mailDto.getFrom());
    assertEquals("customer@example.com", mailDto.getTo());
    assertEquals("Factura electrónica 001-002-000000123", mailDto.getSubject());
    assertTrue(
        mailDto.getTemplateHtml().contains("Cliente Demo"), "HTML body should contain buyer name");
    assertTrue(
        mailDto.getTemplateHtml().contains("001-002-000000123"),
        "HTML body should contain invoice number");

    List<AttachmentDto> attachments = mailDto.getListAttachmentDtos();
    assertEquals(2, attachments.size());
    assertEquals("FAC 001-002-000000123.pdf", attachments.get(0).getLabel());
    assertEquals("FAC 001-002-000000123.xml", attachments.get(1).getLabel());
  }

  @Test
  void shouldSkipEmailWhenRecipientIsEmpty() {
    Integer result =
        invoiceEmailService.sendAuthorizedInvoice(
            "   ",
            new Invoice(),
            new InvoiceDto(),
            new AuthorizationSri(),
            "pdf".getBytes(StandardCharsets.UTF_8),
            "<xml/>".getBytes(StandardCharsets.UTF_8));

    assertEquals(0, result);
    verify(emailUtil, never()).sendMailWithAttachment(Mockito.any());
  }
}
