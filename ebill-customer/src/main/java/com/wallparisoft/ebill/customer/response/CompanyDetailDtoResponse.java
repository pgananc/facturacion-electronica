package com.wallparisoft.ebill.customer.response;

import com.wallparisoft.ebill.customer.dto.CompanyDetailsDto;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import java.util.List;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class CompanyDetailDtoResponse extends BasicResponse {
  private List<CompanyDetailsDto> companyDetailsDtos;
  private CompanyDetailsDto companyDetailsDto;
}
