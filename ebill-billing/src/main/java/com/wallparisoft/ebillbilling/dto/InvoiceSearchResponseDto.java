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
public class InvoiceSearchResponseDto {

  private Long idInvoice;

  private String invoiceNumber;

  private LocalDateTime creationDate;

  private Long idClient;

  private String nameClient;

  private BigDecimal totalAmount;

  private String authorizationNumber;

  private String receptionStatusSri;

  private String authorizationStatusSri;

  /**
   * Ruta donde se guardó el XML y PDF de la factura autorizada Ejemplo:
   * "/home/invoice/1234567890/2025/11/11"
   */
  private String storagePath;
}
