package com.wallparisoft.ebill.auth.util.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyEstablishment {
  private Long idCompany;
  private List<Long> establishments;
}
