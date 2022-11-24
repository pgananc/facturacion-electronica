package com.wallparisoft.ebill.customer.mapper;


import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.entity.Company;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class CompanyMapper {
    public abstract Company convertCompanyDtoToCompany(CompanyDto companyDto);

    @InheritInverseConfiguration
    public abstract CompanyDto convertCompanyToCompanyDto(Company company);

    public List<CompanyDto> convertCompaniesToCompaniesDto(List<Company> companies) {
        return companies.stream().map(company -> convertCompanyToCompanyDto(company)).collect(Collectors.toList());
    }
}
