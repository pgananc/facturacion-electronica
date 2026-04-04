package com.wallparisoft.ebill.customer.dto;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstablishmentResponseDto {

  private Long idEstablishment;
  private String name;
  private String code;
  private String address;
  private String type;
  private Boolean status;
  private Long companyId; // Evitamos exponer toda la entidad Company
  private Set<InvoiceIssuancePointResponseDto> invoiceIssuancePoints;
}
