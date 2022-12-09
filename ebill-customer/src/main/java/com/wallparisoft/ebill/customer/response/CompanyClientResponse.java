package com.wallparisoft.ebill.customer.response;


import com.wallparisoft.ebill.customer.dto.CompanyClientDto;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;


@SuperBuilder
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompanyClientResponse extends BasicResponse {
    List<CompanyClientDto> companyClientDtos;
    CompanyClientDto companyClientDto;
}
