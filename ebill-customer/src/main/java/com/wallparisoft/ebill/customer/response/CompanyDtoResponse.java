package com.wallparisoft.ebill.customer.response;


import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.CompanyDto;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;


@SuperBuilder
@Getter
public class CompanyDtoResponse extends BasicResponse {
    private List<CompanyDto> companyDtos;
}
