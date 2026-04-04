package com.wallparisoft.ebillbilling.mapper;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.CreditNoteDto;
import com.wallparisoft.ebillbilling.dto.InvoiceDetailDto;
import com.wallparisoft.ebillbilling.dto.ProductDto;
import com.wallparisoft.ebillbilling.entity.CreditNote;
import com.wallparisoft.ebillbilling.entity.CreditNoteDetail;
import com.wallparisoft.ebillbilling.response.CreditNoteResponse;
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
public class CreditNoteServiceMapper {

  public CreditNote toCreditNote(CreditNoteDto creditNoteDto) {
    // Extract idCompany
    Long idCompany =
        creditNoteDto.getIdCompany() != null
            ? creditNoteDto.getIdCompany()
            : (creditNoteDto.getCompanyDto() != null
                ? creditNoteDto.getCompanyDto().getIdCompany()
                : null);

    // Extract idClient
    Long idClient =
        creditNoteDto.getIdClient() != null
            ? creditNoteDto.getIdClient()
            : (creditNoteDto.getClientDto() != null
                ? creditNoteDto.getClientDto().getIdClient()
                : null);

    // Extraer originalInvoiceId de cualquiera de los dos campos
    Long originalInvoiceId =
        creditNoteDto.getOriginalInvoiceId() != null
            ? creditNoteDto.getOriginalInvoiceId()
            : creditNoteDto.getIdInvoice();
    // Extract idEstablishment desde EstablishmentDto si está presente
    Long idEstablishment =
        creditNoteDto.getEstablishment() != null
            ? creditNoteDto.getEstablishment().getIdEstablishment()
            : null;
    CreditNote creditNote =
        CreditNote.builder()
            .status(true)
            .idCreditNote(creditNoteDto.getIdCreditNote())
            .idCompany(idCompany)
            .idClient(idClient)
            .idIssuancePoint(creditNoteDto.getIdIssuancePoint())
            .idEstablishment(idEstablishment)
            .sequential(creditNoteDto.getSequential())
            .creditNoteNumber(creditNoteDto.getCreditNoteNumber())
            .additionalInformation(creditNoteDto.getAdditionalInformation())
            .environment(creditNoteDto.getEnvironment())
            // Totales
            .totalAmount(creditNoteDto.getTotalAmount())
            .subtotalIva(creditNoteDto.getSubtotalIva())
            .subtotalZero(creditNoteDto.getSubtotalZero())
            .subtotalIce(creditNoteDto.getSubtotalIce())
            .discount(creditNoteDto.getDiscount())
            .totalIva(creditNoteDto.getTotalIva())
            .totalIce(creditNoteDto.getTotalIce())
            .tip(creditNoteDto.getTip())
            .valorModificacion(creditNoteDto.getValorModificacion())
            // Observación
            .observation(creditNoteDto.getObservation())
            // Datos de factura original
            .originalInvoiceId(originalInvoiceId)
            .originalAccessKey(creditNoteDto.getOriginalAccessKey())
            .originalInvoiceNumber(creditNoteDto.getOriginalInvoiceNumber())
            // Convertir LocalDate a LocalDateTime
            .originalIssueDate(
                creditNoteDto.getOriginalIssueDate() != null
                    ? creditNoteDto.getOriginalIssueDate().atStartOfDay()
                    : null)
            .documentTypeModified(creditNoteDto.getDocumentTypeModified())
            .reason(creditNoteDto.getReason())
            // Convertir LocalDate a LocalDateTime
            .issueDate(
                creditNoteDto.getIssueDate() != null
                    ? creditNoteDto.getIssueDate().atStartOfDay()
                    : null)
            .build();

    // Map credit note details
    List<CreditNoteDetail> creditNoteDetails = new ArrayList<>();
    List<InvoiceDetailDto> detailsSource =
        creditNoteDto.getCreditNoteDetails() != null
            ? creditNoteDto.getCreditNoteDetails()
            : creditNoteDto.getDetails();

    if (detailsSource != null) {
      creditNoteDetails =
          detailsSource.stream()
              .map(
                  detail -> {
                    // Extraer idProduct de productDto o directamente del campo
                    Long idProduct =
                        detail.getIdProduct() != null
                            ? detail.getIdProduct()
                            : (detail.getProductDto() != null
                                ? detail.getProductDto().getIdProduct()
                                : null);

                    // Extraer mainCode de varios posibles campos
                    String mainCode =
                        detail.getMainCode() != null
                            ? detail.getMainCode()
                            : (detail.getCode() != null
                                ? detail.getCode()
                                : (detail.getProductDto() != null
                                    ? detail.getProductDto().getMainCode()
                                    : null));

                    // Extraer description de varios posibles campos
                    String description =
                        detail.getDescription() != null
                            ? detail.getDescription()
                            : (detail.getProductDto() != null
                                ? detail.getProductDto().getName()
                                : null);

                    // Extraer ivaPercentage (del detalle o de la rate del producto)
                    BigDecimal ivaPercentage =
                        detail.getIvaPercentage() != null
                            ? detail.getIvaPercentage()
                            : (detail.getProductDto() != null
                                ? detail.getProductDto().getRate()
                                : null);

                    return CreditNoteDetail.builder()
                        .status(true)
                        .creditNote(creditNote)
                        .idProduct(idProduct)
                        .idInvoiceDetail(detail.getIdInvoiceDetail())
                        .mainCode(mainCode)
                        .auxCode(detail.getAuxCode())
                        .description(description)
                        .unitPrice(detail.getUnitPrice())
                        .discount(detail.getDiscount())
                        .subtotal(detail.getSubtotal())
                        .quantity(detail.getQuantity())
                        .ivaPercentage(ivaPercentage)
                        .ivaValue(detail.getIvaValue())
                        .iceValue(detail.getIceValue())
                        .build();
                  })
              .collect(Collectors.toList());
    }
    creditNote.setCreditNoteDetails(creditNoteDetails);

    return creditNote;
  }

  public CreditNoteDto toCreditNoteDto(CreditNote creditNote) {
    CreditNoteDto creditNoteDto =
        CreditNoteDto.builder()
            .status(creditNote.getStatus() != null ? creditNote.getStatus().toString() : "true")
            .idCreditNote(creditNote.getIdCreditNote())
            .idCompany(creditNote.getIdCompany())
            .idClient(creditNote.getIdClient())
            .idIssuancePoint(creditNote.getIdIssuancePoint())
            .sequential(creditNote.getSequential())
            .creditNoteNumber(creditNote.getCreditNoteNumber())
            .additionalInformation(creditNote.getAdditionalInformation())
            .environment(creditNote.getEnvironment())
            // Totales
            .totalAmount(creditNote.getTotalAmount())
            .subtotalIva(creditNote.getSubtotalIva())
            .subtotalZero(creditNote.getSubtotalZero())
            .subtotalIce(creditNote.getSubtotalIce())
            .discount(creditNote.getDiscount())
            .totalIva(creditNote.getTotalIva())
            .totalIce(creditNote.getTotalIce())
            .tip(creditNote.getTip())
            .valorModificacion(creditNote.getValorModificacion())
            // Observación
            .observation(creditNote.getObservation())
            // Datos de factura original
            .originalInvoiceId(creditNote.getOriginalInvoiceId())
            .idInvoice(creditNote.getOriginalInvoiceId()) // Alias
            .originalAccessKey(creditNote.getOriginalAccessKey())
            .originalInvoiceNumber(creditNote.getOriginalInvoiceNumber())
            // Convertir LocalDateTime a LocalDate
            .originalIssueDate(
                creditNote.getOriginalIssueDate() != null
                    ? creditNote.getOriginalIssueDate().toLocalDate()
                    : null)
            .documentTypeModified(creditNote.getDocumentTypeModified())
            .reason(creditNote.getReason())
            .idReason(
                creditNote.getCreditNoteReason() != null
                    ? creditNote.getCreditNoteReason().getIdReason()
                    : null)
            // Convertir LocalDateTime a LocalDate
            .issueDate(
                creditNote.getIssueDate() != null ? creditNote.getIssueDate().toLocalDate() : null)
            // Initialize @Nonnull fields
            .companyDto(new CompanyDto())
            .clientDto(new ClientDto())
            .build();

    // Map credit note details
    List<InvoiceDetailDto> creditNoteDetailDtos = new ArrayList<>();
    if (creditNote.getCreditNoteDetails() != null) {
      creditNoteDetailDtos =
          creditNote.getCreditNoteDetails().stream()
              .map(
                  detail ->
                      InvoiceDetailDto.builder()
                          .idInvoiceDetail(detail.getIdInvoiceDetail())
                          .idProduct(detail.getIdProduct())
                          .mainCode(detail.getMainCode())
                          .code(detail.getMainCode()) // Alias
                          .auxCode(detail.getAuxCode())
                          .description(detail.getDescription())
                          .unitPrice(detail.getUnitPrice())
                          .discount(detail.getDiscount())
                          .subtotal(detail.getSubtotal())
                          .quantity(detail.getQuantity())
                          .ivaPercentage(detail.getIvaPercentage())
                          .ivaValue(detail.getIvaValue())
                          .iceValue(detail.getIceValue())
                          .productDto(new ProductDto()) // Will be
                          // populated
                          // by
                          // service
                          .build())
              .collect(Collectors.toList());
    }
    creditNoteDto.setCreditNoteDetails(creditNoteDetailDtos);
    creditNoteDto.setDetails(creditNoteDetailDtos); // Alias

    return creditNoteDto;
  }

  public CreditNoteResponse toCreditNoteResponse(CreditNote creditNote) {
    CreditNoteResponse response =
        CreditNoteResponse.builder()
            .idCreditNote(creditNote.getIdCreditNote())
            .creditNoteNumber(creditNote.getCreditNoteNumber())
            .build();

    // Incluir datos de autorización si existen (igual que en facturas)
    if (creditNote.getAuthorizationSri() != null) {
      // Campos directos en el response (como en InvoiceResponse)
      response.setAccessKey(creditNote.getAuthorizationSri().getAccessKey());
      response.setAuthorizationNumber(creditNote.getAuthorizationSri().getAuthorizationNumber());
      response.setAuthorizationDate(creditNote.getAuthorizationSri().getAuthorizationDate());
      response.setReceptionStatusSri(creditNote.getAuthorizationSri().getReceptionStatusSri());
      response.setAuthorizationStatusSri(
          creditNote.getAuthorizationSri().getAuthorizationStatusSri());
      response.setStoragePath(creditNote.getAuthorizationSri().getAuthorizationStoragePath());

      // También incluir en el DTO para compatibilidad
      CreditNoteDto dto =
          CreditNoteDto.builder()
              .idCreditNote(creditNote.getIdCreditNote())
              .sequential(creditNote.getSequential())
              .creditNoteNumber(creditNote.getCreditNoteNumber())
              .accessKey(creditNote.getAuthorizationSri().getAccessKey())
              .authorizationNumber(creditNote.getAuthorizationSri().getAuthorizationNumber())
              // Inicializar campos @NonNull para evitar NullPointerException
              .companyDto(new CompanyDto())
              .clientDto(new ClientDto())
              .build();
      response.setCreditNoteDto(dto);

      // Agregar información del estado al mensaje
      String status = creditNote.getAuthorizationSri().getAuthorizationStatusSri();
      if ("AUTORIZADO".equalsIgnoreCase(status)) {
        response.setMessage("Nota de crédito AUTORIZADA por el SRI");
      } else if ("NO AUTORIZADO".equalsIgnoreCase(status)) {
        response.setMessage(
            "Nota de crédito NO AUTORIZADA por el SRI: "
                + creditNote.getAuthorizationSri().getAuthorizationError());
      } else if ("RECIBIDA"
          .equalsIgnoreCase(creditNote.getAuthorizationSri().getReceptionStatusSri())) {
        response.setMessage("Nota de crédito RECIBIDA por el SRI, pendiente de autorización");
      } else {
        response.setMessage("Estado: " + status);
      }
    } else {
      response.setMessage("Nota de crédito guardada como borrador");
    }

    return response;
  }
}
