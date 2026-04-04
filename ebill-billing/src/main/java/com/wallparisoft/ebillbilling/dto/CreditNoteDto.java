package com.wallparisoft.ebillbilling.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditNoteDto {

  private Long idCreditNote;

  private Long idCompany;

  private Long idClient;

  private Long idIssuancePoint;

  private Long sequential;

  @Nonnull private CompanyDto companyDto;
  @Nonnull private ClientDto clientDto;

  private LocalDateTime creationDate;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate issueDate;

  private String accessKey;

  private String creditNoteNumber;

  private String authorizationNumber;

  private String additionalInformation;

  private LocalDateTime authorizationDate;

  private BigDecimal totalAmount;

  private BigDecimal discount;

  private BigDecimal subtotalZero;

  private BigDecimal subtotalIva;

  private BigDecimal subtotalIce;

  private BigDecimal subtotalDiscount;

  private BigDecimal totalIva;

  private BigDecimal totalIce;

  private BigDecimal tip;

  private BigDecimal valorModificacion;

  private String environment;

  private String status;

  private List<InvoiceDetailDto> creditNoteDetails;

  private EstablishmentDto establishment;

  // Campos específicos de Nota de Crédito
  /** ID de la factura original que se modifica/anula */
  private Long originalInvoiceId;

  /** Alias de originalInvoiceId para compatibilidad con frontend */
  private Long idInvoice;

  /** Clave de acceso de la factura original */
  private String originalAccessKey;

  /** Número de factura original (formato: estab-ptoEmi-secuencial) */
  private String originalInvoiceNumber;

  /** Fecha de emisión de la factura original */
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate originalIssueDate;

  /** Tipo de documento modificado (01=Factura, 03=Liquidación, etc.) */
  private String documentTypeModified;

  /** ID del motivo de nota de crédito (referencia al catálogo credit_note_reason) */
  private Long idReason;

  /** Motivo de la nota de crédito (texto descriptivo) */
  private String reason;

  /** Observación adicional */
  private String observation;

  // Alias para compatibilidad con frontend
  private List<InvoiceDetailDto> details; // Alias de creditNoteDetails
}
