package com.wallparisoft.ebill.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceIssuancePointResponseDto {
  private Long idInvoiceIssuancePoint;
  private String code;
  private Long sequentialInvoice;
  private Long establishmentId;
  private boolean status;
}
