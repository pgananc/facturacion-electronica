package com.wallparisoft.ebill.customer.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CompanyDetailsDto extends CompanyDto {
  private List<ContactDto> contacts;
  private List<Long> companiesIds;
  private List<EstablishmentResponseDto> establishments;
  private Boolean isSuperAdmin;
}
