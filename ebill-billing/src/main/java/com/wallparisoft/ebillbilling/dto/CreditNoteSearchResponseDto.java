package com.wallparisoft.ebillbilling.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class CreditNoteSearchResponseDto {

  private Long idCreditNote;

  private String creditNoteNumber;

  private LocalDateTime creationDate;

  private Long idClient;

  private String nameClient;

  private BigDecimal totalAmount;

  private String authorizationNumber;

  private String receptionStatusSri;

  private String authorizationStatusSri;

  /**
   * Ruta donde se guardó el XML y PDF de la nota de crédito autorizada Ejemplo:
   * "/home/credit_notes/1234567890/2025/11/11"
   */
  private String storagePath;

  /** ID de la factura original */
  private Long originalInvoiceId;

  /** Número de la factura original */
  private String originalInvoiceNumber;

  /** Motivo de la nota de crédito */
  private String reason;
}
