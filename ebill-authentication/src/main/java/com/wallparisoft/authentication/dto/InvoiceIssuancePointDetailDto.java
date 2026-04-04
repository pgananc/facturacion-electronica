package com.wallparisoft.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para la información detallada de un punto de emisión. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceIssuancePointDetailDto {
  private Long idInvoiceIssuancePoint;
  private String code;
  private Long idEstablishment;
  private Long sequentialInvoice;
  private Long sequentialCreditNote;
  private Long sequentialDebitNote;
  private Long sequentialRetention;
  private Long sequentialGuideRemision;
  private Boolean status;
}
