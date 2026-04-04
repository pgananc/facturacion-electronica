package com.wallparisoft.ebill.customer.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchCompanyDto {

  String identification;
  String name;
  Boolean status;
  private List<Long> companiesIds;
  private Boolean isSuperAdmin;
}
