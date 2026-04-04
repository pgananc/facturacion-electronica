package com.wallparisoft.ebill.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un Establishment con su InvoiceIssuancePoint asociado. Esta estructura es
 * compatible con EstablishmentDto de ebill-billing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstablishmentWithIssuancePointDto {

  private Long idEstablishment;
  private String name;
  private String code;
  private String address;
  private String type; // "MATRIZ" o "SUCURSAL"
  private String parentAddress; // Dirección del establecimiento padre (si es SUCURSAL)
  private IssuancePointDto issuancePoint;
}
