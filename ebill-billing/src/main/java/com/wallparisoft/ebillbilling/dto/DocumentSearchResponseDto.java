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
public class DocumentSearchResponseDto {

  /** Tipo de documento: 1 = Factura, 2 = Nota de Crédito */
  private Integer documentType;

  /** ID del documento (idInvoice o idCreditNote según el tipo) */
  private Long idDocument;

  /** Número del documento (invoiceNumber o creditNoteNumber) */
  private String documentNumber;

  private LocalDateTime creationDate;

  private Long idClient;

  private String nameClient;

  private BigDecimal totalAmount;

  private String authorizationNumber;

  private String receptionStatusSri;

  private String authorizationStatusSri;

  /**
   * Ruta donde se guardó el XML y PDF del documento autorizado Ejemplo:
   * "/home/invoice/1234567890/2025/11/11" o "/home/credit_notes/1234567890/2025/11/11"
   */
  private String storagePath;

  // Campos específicos de Nota de Crédito (null para facturas)
  /** ID de la factura original (solo para notas de crédito) */
  private Long originalInvoiceId;

  /** Número de la factura original (solo para notas de crédito) */
  private String originalInvoiceNumber;

  /** Motivo de la nota de crédito (solo para notas de crédito) */
  private String reason;
}
