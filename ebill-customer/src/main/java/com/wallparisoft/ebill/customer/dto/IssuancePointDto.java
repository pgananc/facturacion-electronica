package com.wallparisoft.ebill.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un punto de emisión (InvoiceIssuancePoint). Compatible con IssuancePointDto de
 * ebill-billing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssuancePointDto {

  private Long idIssuancePoint;
  private String code;
  private String sequentialInvoice;
}
