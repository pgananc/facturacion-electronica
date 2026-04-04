package com.wallparisoft.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para la información completa de un establecimiento. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstablishmentDto {
  private Long idEstablishment;
  private String code;
  private String name;
  private String address;
  private Long idCompany;
  private Boolean status;
}
