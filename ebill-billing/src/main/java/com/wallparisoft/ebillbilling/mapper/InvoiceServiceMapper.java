package com.wallparisoft.ebillbilling.mapper;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDetailDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDto;
import com.wallparisoft.ebillbilling.dto.PaymentDto;
import com.wallparisoft.ebillbilling.dto.ProductDto;
import com.wallparisoft.ebillbilling.entity.Invoice;
import com.wallparisoft.ebillbilling.entity.InvoiceDetail;
import com.wallparisoft.ebillbilling.entity.InvoicePayment;
import com.wallparisoft.ebillbilling.entity.PaymentMethod;
import com.wallparisoft.ebillbilling.repository.IPaymentMethodRepo;
import com.wallparisoft.ebillbilling.response.InvoiceResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE)
public class InvoiceServiceMapper {

  final IPaymentMethodRepo paymentMethodRepo;

  public Invoice toInvoice(InvoiceDto invoiceDto) {

    // Extract idCompany (priority: direct field, then from companyDto)
    Long idCompany =
        invoiceDto.getIdCompany() != null
            ? invoiceDto.getIdCompany()
            : (invoiceDto.getCompanyDto() != null
                ? invoiceDto.getCompanyDto().getIdCompany()
                : null);

    // Extract idClient (priority: direct field, then from clientDto)
    Long idClient =
        invoiceDto.getIdClient() != null
            ? invoiceDto.getIdClient()
            : (invoiceDto.getClientDto() != null ? invoiceDto.getClientDto().getIdClient() : null);

    // Extract idEstablishment desde EstablishmentDto si está presente
    Long idEstablishment =
        invoiceDto.getEstablishment() != null
            ? invoiceDto.getEstablishment().getIdEstablishment()
            : null;

    // Si no viene issueDate, usar la fecha/hora actual
    // Convertir LocalDate a LocalDateTime (inicio del día) si viene del DTO
    java.time.LocalDateTime issueDate =
        invoiceDto.getIssueDate() != null
            ? invoiceDto.getIssueDate().atStartOfDay()
            : java.time.LocalDateTime.now();

    Invoice invoice =
        Invoice.builder()
            .status(true)
            .idInvoice(invoiceDto.getIdInvoice())
            .idCompany(idCompany)
            .idClient(idClient)
            .idEstablishment(idEstablishment)
            .invoiceNumber(invoiceDto.getInvoiceNumber())
            .issueDate(issueDate)
            .additionalInformation(invoiceDto.getAdditionalInformation())
            .environment(invoiceDto.getEnvironment())
            .totalAmount(invoiceDto.getTotalAmount())
            .subtotalIva(invoiceDto.getSubtotalIva())
            .subtotalZero(invoiceDto.getSubtotalZero())
            .subtotalIce(invoiceDto.getSubtotalIce())
            .tip(invoiceDto.getTip())
            .discount(invoiceDto.getDiscount())
            .build();
    List<InvoiceDetail> invoiceDetails = new ArrayList<>();
    invoice.setInvoiceDetails(new ArrayList<>());
    invoiceDetails =
        invoiceDto.getInvoiceDetails().stream()
            .map(
                invoiceDetail ->
                    InvoiceDetail.builder()
                        .status(true)
                        .discount(invoiceDetail.getDiscount())
                        .invoice(invoice)
                        .idProduct(invoiceDetail.getProductDto().getIdProduct())
                        .unitPrice(invoiceDetail.getUnitPrice())
                        .discount(invoiceDetail.getDiscount())
                        .subtotal(invoiceDetail.getSubtotal())
                        .quantity(invoiceDetail.getQuantity())
                        .build())
            .collect(Collectors.toList());
    if (invoiceDto.getIdInvoice() != null && invoiceDto.getIdInvoice() > 0) {
      invoice.setIdInvoice(invoiceDto.getIdInvoice());
    }
    invoice.getInvoiceDetails().addAll(invoiceDetails);

    // Mapear formas de pago (paymentMethods)
    if (invoiceDto.getPaymentMethods() != null && !invoiceDto.getPaymentMethods().isEmpty()) {
      List<InvoicePayment> invoicePayments = new ArrayList<>();
      for (PaymentDto paymentDto : invoiceDto.getPaymentMethods()) {
        PaymentMethod paymentMethodEntity = null;

        // Intentar obtener PaymentMethod por ID o por código
        if (paymentDto.getIdPaymentMethod() != null) {
          paymentMethodEntity =
              paymentMethodRepo.findById(paymentDto.getIdPaymentMethod()).orElse(null);
        } else if (paymentDto.getPaymentMethodCode() != null) {
          paymentMethodEntity =
              paymentMethodRepo
                  .findByPaymentMethodCode(paymentDto.getPaymentMethodCode())
                  .orElse(null);
        }

        InvoicePayment invoicePayment =
            InvoicePayment.builder()
                .invoice(invoice)
                .paymentMethod(paymentMethodEntity) // Relación, no código duplicado
                .total(
                    paymentDto.getTotal() != null ? paymentDto.getTotal() : paymentDto.getValue())
                .term(paymentDto.getTerm() != null ? paymentDto.getTerm() : 0)
                .timeUnit(
                    paymentDto.getTimeUnit() != null
                        ? paymentDto.getTimeUnit()
                        : paymentDto.getTime())
                .status(true)
                .build();

        invoicePayments.add(invoicePayment);
      }
      invoice.setInvoicePayments(invoicePayments);
    } else {
      invoice.setInvoicePayments(new ArrayList<>());
    }

    return invoice;
  }

  public InvoiceDto toInvoiceDto(Invoice invoice) {
    InvoiceDto invoiceDto =
        InvoiceDto.builder()
            .status(invoice.getStatus().toString())
            .idInvoice(invoice.getIdInvoice())
            .idCompany(invoice.getIdCompany())
            .idClient(invoice.getIdClient())
            .invoiceNumber(invoice.getInvoiceNumber())
            .issueDate(invoice.getIssueDate().toLocalDate())
            .additionalInformation(invoice.getAdditionalInformation())
            .environment(invoice.getEnvironment())
            .totalAmount(invoice.getTotalAmount())
            .subtotalIva(invoice.getSubtotalIva())
            .subtotalZero(invoice.getSubtotalZero())
            .subtotalIce(invoice.getSubtotalIce())
            .tip(invoice.getTip())
            .discount(invoice.getDiscount())
            // Initialize @Nonnull fields to avoid NullPointerException
            .companyDto(new CompanyDto())
            .clientDto(new ClientDto())
            .productDto(new ProductDto())
            .build();
    List<InvoiceDetailDto> invoiceDetailDtos = new ArrayList<>();
    invoiceDto.setInvoiceDetails(new ArrayList<>());
    invoiceDetailDtos =
        invoice.getInvoiceDetails().stream()
            .map(
                invoiceDetail ->
                    InvoiceDetailDto.builder()
                        .discount(invoiceDetail.getDiscount())
                        .idProduct(invoiceDetail.getIdProduct())
                        .unitPrice(invoiceDetail.getUnitPrice())
                        .discount(invoiceDetail.getDiscount())
                        .subtotal(invoiceDetail.getSubtotal())
                        .quantity(invoiceDetail.getQuantity())
                        .build())
            .collect(Collectors.toList());
    if (invoice.getIdInvoice() != null && invoice.getIdInvoice() > 0) {
      invoiceDto.setIdInvoice(invoice.getIdInvoice());
    }
    invoiceDto.getInvoiceDetails().addAll(invoiceDetailDtos);

    // Mapear formas de pago desde invoicePayments
    if (invoice.getInvoicePayments() != null && !invoice.getInvoicePayments().isEmpty()) {
      List<PaymentDto> paymentDtos =
          invoice.getInvoicePayments().stream()
              .map(
                  invoicePayment -> {
                    PaymentMethod pm = invoicePayment.getPaymentMethod();
                    return PaymentDto.builder()
                        .idPayment(invoicePayment.getIdInvoicePayment())
                        .idPaymentMethod(pm != null ? pm.getIdPaymentMethod() : null)
                        .paymentMethodCode(
                            pm != null ? pm.getPaymentMethodCode() : null) // Obtenido desde
                        // PaymentMethod
                        .paymentType(pm != null ? pm.getPaymentMethodType() : null)
                        .total(invoicePayment.getTotal())
                        .value(invoicePayment.getTotal()) // Alias
                        .term(invoicePayment.getTerm())
                        .timeUnit(invoicePayment.getTimeUnit())
                        .time(invoicePayment.getTimeUnit()) // Alias
                        .build();
                  })
              .collect(Collectors.toList());
      invoiceDto.setPaymentMethods(paymentDtos);
    } else {
      invoiceDto.setPaymentMethods(new ArrayList<>());
    }

    return invoiceDto;
  }

  public InvoiceResponse toInvoiceResponse(Invoice invoice) {
    InvoiceResponse.InvoiceResponseBuilder builder =
        InvoiceResponse.builder()
            .idInvoice(invoice.getIdInvoice())
            .invoiceNumber(invoice.getInvoiceNumber());

    // Solo agregar accessKey si la factura tiene authorizationSri (no es borrador)
    if (invoice.getAuthorizationSri() != null) {
      builder.accessKey(invoice.getAuthorizationSri().getAccessKey());
    }

    return builder.build();
  }
}
