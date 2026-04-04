package com.wallparisoft.ebill.customer.mapper;

import com.wallparisoft.ebill.customer.dto.CompanyDetailsDto;
import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.entity.Company;
import java.util.List;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

  Company convertCompanyDtoToCompany(CompanyDto companyDto);

  @Named("convertCompanyToCompanyDetailsDto")
  @Mapping(target = "contacts", ignore = true)
  @Mapping(target = "companiesIds", ignore = true)
  @Mapping(target = "isSuperAdmin", ignore = true)
  CompanyDetailsDto convertCompanyToCompanyDetailsDto(Company company);

  @Named("convertCompanyToCompanyDto")
  CompanyDto entityToDto(Company company);

  @IterableMapping(qualifiedByName = "convertCompanyToCompanyDto")
  List<CompanyDto> convertCompaniesToCompaniesDto(List<Company> companies);

  @IterableMapping(qualifiedByName = "convertCompanyToCompanyDetailsDto")
  List<CompanyDetailsDto> convertCompaniesToCompanyDetailsDtos(List<Company> companies);
}
