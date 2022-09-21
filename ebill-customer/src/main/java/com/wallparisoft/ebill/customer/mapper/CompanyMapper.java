package com.wallparisoft.ebill.customer.mapper;


import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.entity.Company;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    CompanyMapper INSTANCE = Mappers.getMapper(CompanyMapper.class);
    Company convertCompanyDtoToCompany(CompanyDto companyDto);
    @InheritInverseConfiguration
    CompanyDto convertCompanyToCompanyDto(Company company);
}
