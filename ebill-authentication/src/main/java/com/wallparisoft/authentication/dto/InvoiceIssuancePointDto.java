package com.wallparisoft.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceIssuancePointDto {
  private Long idInvoiceIssuancePoint;
  private String code;
  private Long establishmentId;
  private Long sequentialInvoice;
  private boolean status;
}
