package com.wallparisoft.ebillbilling.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EstablishmentDto {
  private Long idEstablishment;
  private String name;
  private String code;
  private String address;
  private String type; // "MATRIZ" o "SUCURSAL"
  private String parentAddress; // Dirección del establecimiento padre (si es SUCURSAL)
  private IssuancePointDto issuancePoint;
}
