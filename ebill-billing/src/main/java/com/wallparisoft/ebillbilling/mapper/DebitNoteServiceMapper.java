package com.wallparisoft.ebillbilling.mapper;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.DebitNoteDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDetailDto;
import com.wallparisoft.ebillbilling.dto.PaymentDto;
import com.wallparisoft.ebillbilling.dto.ProductDto;
import com.wallparisoft.ebillbilling.entity.DebitNote;
import com.wallparisoft.ebillbilling.entity.DebitNoteDetail;
import com.wallparisoft.ebillbilling.entity.DebitNotePayment;
import com.wallparisoft.ebillbilling.entity.PaymentMethod;
import com.wallparisoft.ebillbilling.response.DebitNoteResponse;
import java.math.BigDecimal;
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
public class DebitNoteServiceMapper {

  public DebitNote toDebitNote(DebitNoteDto debitNoteDto) {
    // Extract idCompany
    Long idCompany =
        debitNoteDto.getIdCompany() != null
            ? debitNoteDto.getIdCompany()
            : (debitNoteDto.getCompanyDto() != null
                ? debitNoteDto.getCompanyDto().getIdCompany()
                : null);

    // Extract idClient
    Long idClient =
        debitNoteDto.getIdClient() != null
            ? debitNoteDto.getIdClient()
            : (debitNoteDto.getClientDto() != null
                ? debitNoteDto.getClientDto().getIdClient()
                : null);

    // Extraer originalInvoiceId de cualquiera de los dos campos
    Long originalInvoiceId =
        debitNoteDto.getOriginalInvoiceId() != null
            ? debitNoteDto.getOriginalInvoiceId()
            : debitNoteDto.getIdInvoice();

    // Extract idEstablishment desde EstablishmentDto si está presente
    Long idEstablishment =
        debitNoteDto.getEstablishment() != null
            ? debitNoteDto.getEstablishment().getIdEstablishment()
            : null;

    DebitNote debitNote =
        DebitNote.builder()
            .status(true)
            .idDebitNote(debitNoteDto.getIdDebitNote())
            .idCompany(idCompany)
            .idClient(idClient)
            .idIssuancePoint(debitNoteDto.getIdIssuancePoint())
            .idEstablishment(idEstablishment)
            .sequential(debitNoteDto.getSequential())
            .debitNoteNumber(debitNoteDto.getDebitNoteNumber())
            .additionalInformation(debitNoteDto.getAdditionalInformation())
            // environment se asigna desde SriConfiguration en el servicio, no desde el DTO
            .environment(debitNoteDto.getEnvironment())
            // Totales
            .totalAmount(debitNoteDto.getTotalAmount())
            .subtotalIva(debitNoteDto.getSubtotalIva())
            .subtotalZero(debitNoteDto.getSubtotalZero())
            .subtotalIce(debitNoteDto.getSubtotalIce())
            .discount(debitNoteDto.getDiscount())
            .totalIva(debitNoteDto.getTotalIva())
            .totalIce(debitNoteDto.getTotalIce())
            .valorModificacion(debitNoteDto.getValorModificacion())
            .valorTotal(debitNoteDto.getValorTotal())
            // Observación
            .observation(debitNoteDto.getObservation())
            // Datos de factura original
            .originalInvoiceId(originalInvoiceId)
            .originalAccessKey(debitNoteDto.getOriginalAccessKey())
            .originalInvoiceNumber(debitNoteDto.getOriginalInvoiceNumber())
            // Convertir LocalDate a LocalDateTime
            .originalIssueDate(
                debitNoteDto.getOriginalIssueDate() != null
                    ? debitNoteDto.getOriginalIssueDate().atStartOfDay()
                    : null)
            .documentTypeModified(debitNoteDto.getDocumentTypeModified())
            // Convertir LocalDate a LocalDateTime
            .issueDate(
                debitNoteDto.getIssueDate() != null
                    ? debitNoteDto.getIssueDate().atStartOfDay()
                    : null)
            .build();

    // Map debit note details
    List<DebitNoteDetail> debitNoteDetails = new ArrayList<>();
    List<InvoiceDetailDto> detailsSource =
        debitNoteDto.getDebitNoteDetails() != null
            ? debitNoteDto.getDebitNoteDetails()
            : debitNoteDto.getDetails();

    if (detailsSource != null) {
      debitNoteDetails =
          detailsSource.stream()
              .map(
                  detail -> {
                    Long idProduct =
                        detail.getIdProduct() != null
                            ? detail.getIdProduct()
                            : (detail.getProductDto() != null
                                ? detail.getProductDto().getIdProduct()
                                : null);

                    return DebitNoteDetail.builder()
                        .status(true)
                        .debitNote(debitNote)
                        .idProduct(idProduct)
                        .unitPrice(detail.getUnitPrice())
                        .discount(
                            detail.getDiscount() != null ? detail.getDiscount() : BigDecimal.ZERO)
                        .subtotal(detail.getSubtotal())
                        .quantity(detail.getQuantity())
                        .build();
                  })
              .collect(Collectors.toList());
    }
    debitNote.setDebitNoteDetails(debitNoteDetails);

    // Map payments
    List<DebitNotePayment> payments = new ArrayList<>();
    if (debitNoteDto.getPaymentMethods() != null && !debitNoteDto.getPaymentMethods().isEmpty()) {
      payments =
          debitNoteDto.getPaymentMethods().stream()
              .map(
                  paymentDto -> {
                    PaymentMethod paymentMethod = new PaymentMethod();
                    paymentMethod.setIdPaymentMethod(paymentDto.getIdPaymentMethod());

                    return DebitNotePayment.builder()
                        .status(true)
                        .debitNote(debitNote)
                        .paymentMethod(paymentMethod)
                        .formaPago(
                            paymentDto.getPaymentMethodCode() != null
                                ? paymentDto.getPaymentMethodCode()
                                : "01")
                        .total(
                            paymentDto.getTotal() != null
                                ? paymentDto.getTotal()
                                : paymentDto.getValue())
                        .term(paymentDto.getTerm())
                        .timeUnit(
                            paymentDto.getTimeUnit() != null
                                ? paymentDto.getTimeUnit()
                                : paymentDto.getTime())
                        .build();
                  })
              .collect(Collectors.toList());
    }
    debitNote.setPayments(payments);

    return debitNote;
  }

  public DebitNoteDto toDebitNoteDto(DebitNote debitNote) {
    DebitNoteDto debitNoteDto =
        DebitNoteDto.builder()
            .status(debitNote.getStatus() != null ? debitNote.getStatus().toString() : "true")
            .idDebitNote(debitNote.getIdDebitNote())
            .idCompany(debitNote.getIdCompany())
            .idClient(debitNote.getIdClient())
            .idIssuancePoint(debitNote.getIdIssuancePoint())
            .sequential(debitNote.getSequential())
            .debitNoteNumber(debitNote.getDebitNoteNumber())
            .additionalInformation(debitNote.getAdditionalInformation())
            .environment(debitNote.getEnvironment())
            // Totales
            .totalAmount(debitNote.getTotalAmount())
            .subtotalIva(debitNote.getSubtotalIva())
            .subtotalZero(debitNote.getSubtotalZero())
            .subtotalIce(debitNote.getSubtotalIce())
            .discount(debitNote.getDiscount())
            .totalIva(debitNote.getTotalIva())
            .totalIce(debitNote.getTotalIce())
            .valorModificacion(debitNote.getValorModificacion())
            .valorTotal(debitNote.getValorTotal())
            // Observación
            .observation(debitNote.getObservation())
            // Datos de factura original
            .originalInvoiceId(debitNote.getOriginalInvoiceId())
            .idInvoice(debitNote.getOriginalInvoiceId()) // Alias
            .originalAccessKey(debitNote.getOriginalAccessKey())
            .originalInvoiceNumber(debitNote.getOriginalInvoiceNumber())
            // Convertir LocalDateTime a LocalDate
            .originalIssueDate(
                debitNote.getOriginalIssueDate() != null
                    ? debitNote.getOriginalIssueDate().toLocalDate()
                    : null)
            .documentTypeModified(debitNote.getDocumentTypeModified())
            // Convertir LocalDateTime a LocalDate
            .issueDate(
                debitNote.getIssueDate() != null ? debitNote.getIssueDate().toLocalDate() : null)
            // Initialize @Nonnull fields
            .companyDto(new CompanyDto())
            .clientDto(new ClientDto())
            .build();

    // Map debit note details
    List<InvoiceDetailDto> debitNoteDetailDtos = new ArrayList<>();
    if (debitNote.getDebitNoteDetails() != null) {
      debitNoteDetailDtos =
          debitNote.getDebitNoteDetails().stream()
              .map(
                  detail ->
                      InvoiceDetailDto.builder()
                          .idProduct(detail.getIdProduct())
                          .unitPrice(detail.getUnitPrice())
                          .discount(detail.getDiscount())
                          .subtotal(detail.getSubtotal())
                          .quantity(detail.getQuantity())
                          .productDto(new ProductDto()) // Will be populated by service
                          .build())
              .collect(Collectors.toList());
    }
    debitNoteDto.setDebitNoteDetails(debitNoteDetailDtos);
    debitNoteDto.setDetails(debitNoteDetailDtos); // Alias

    // Map payments
    List<PaymentDto> paymentDtos = new ArrayList<>();
    if (debitNote.getPayments() != null) {
      paymentDtos =
          debitNote.getPayments().stream()
              .map(
                  payment ->
                      PaymentDto.builder()
                          .idPayment(payment.getIdDebitNotePayment())
                          .idPaymentMethod(
                              payment.getPaymentMethod() != null
                                  ? payment.getPaymentMethod().getIdPaymentMethod()
                                  : null)
                          .paymentMethodCode(payment.getFormaPago())
                          .total(payment.getTotal())
                          .value(payment.getTotal()) // Alias
                          .term(payment.getTerm())
                          .timeUnit(payment.getTimeUnit())
                          .time(payment.getTimeUnit()) // Alias
                          .build())
              .collect(Collectors.toList());
    }
    debitNoteDto.setPaymentMethods(paymentDtos);

    return debitNoteDto;
  }

  public DebitNoteResponse toDebitNoteResponse(DebitNote debitNote) {
    DebitNoteResponse response =
        DebitNoteResponse.builder()
            .idDebitNote(debitNote.getIdDebitNote())
            .debitNoteNumber(debitNote.getDebitNoteNumber())
            .build();

    // Incluir datos de autorización si existen
    if (debitNote.getAuthorizationSri() != null) {
      response.setAccessKey(debitNote.getAuthorizationSri().getAccessKey());
      response.setAuthorizationNumber(debitNote.getAuthorizationSri().getAuthorizationNumber());
      response.setAuthorizationDate(debitNote.getAuthorizationSri().getAuthorizationDate());
      response.setReceptionStatusSri(debitNote.getAuthorizationSri().getReceptionStatusSri());
      response.setAuthorizationStatusSri(
          debitNote.getAuthorizationSri().getAuthorizationStatusSri());
      response.setStoragePath(debitNote.getAuthorizationSri().getAuthorizationStoragePath());

      DebitNoteDto dto =
          DebitNoteDto.builder()
              .idDebitNote(debitNote.getIdDebitNote())
              .sequential(debitNote.getSequential())
              .debitNoteNumber(debitNote.getDebitNoteNumber())
              .accessKey(debitNote.getAuthorizationSri().getAccessKey())
              .authorizationNumber(debitNote.getAuthorizationSri().getAuthorizationNumber())
              .companyDto(new CompanyDto())
              .clientDto(new ClientDto())
              .build();
      response.setDebitNoteDto(dto);

      String status = debitNote.getAuthorizationSri().getAuthorizationStatusSri();
      if ("AUTORIZADO".equalsIgnoreCase(status)) {
        response.setMessage("Nota de débito AUTORIZADA por el SRI");
      } else if ("NO AUTORIZADO".equalsIgnoreCase(status)) {
        response.setMessage(
            "Nota de débito NO AUTORIZADA por el SRI: "
                + debitNote.getAuthorizationSri().getAuthorizationError());
      } else if ("RECIBIDA"
          .equalsIgnoreCase(debitNote.getAuthorizationSri().getReceptionStatusSri())) {
        response.setMessage("Nota de débito RECIBIDA por el SRI, pendiente de autorización");
      } else {
        response.setMessage("Estado: " + status);
      }
    } else {
      response.setMessage("Nota de débito guardada como borrador");
    }

    return response;
  }
}
