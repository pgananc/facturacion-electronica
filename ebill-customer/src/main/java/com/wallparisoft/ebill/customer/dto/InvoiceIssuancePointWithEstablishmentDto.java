package com.wallparisoft.ebill.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceIssuancePointWithEstablishmentDto {

  private Long idInvoiceIssuancePoint;
  private String code;
  private Long establishmentId;
  private String establishmentCode;
  private String establishmentName;
  private Long sequentialInvoice;
  private Boolean status;
}
