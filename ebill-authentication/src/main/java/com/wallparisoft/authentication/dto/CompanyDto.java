package com.wallparisoft.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para la información completa de una compañía. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDto {
  private Long idCompany;
  private String name;
  private String tradeName;
  private String identification;
  private String email;
  private String address;
  private String phone;
  private Boolean status;
}
