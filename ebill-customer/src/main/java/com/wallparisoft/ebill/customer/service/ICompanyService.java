package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.entity.Company;
import com.wallparisoft.ebill.customer.response.CompanyDtoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ICompanyService extends ICRUD<Company> {
    void saveCompanyAndContact(CompanyDto companyDto);

    void updateCompanyAndContact(CompanyDto companyDto, Long idCompany);

    void updateCompany(CompanyDto companyDto);

    void delete(Long id);

    List<CompanyDto> findCompaniesActiveAndContactActive();

    Page<CompanyDto> findCompanyByIdentificationOrNameOrBranchOfficeCode(String identification, String name, String branchOfficeCode, Boolean status, Pageable pageable);

    boolean existsByIdentificationAndBranchOfficeCode(String identification, String branchOfficeCode);

    void saveCompany(CompanyDto companyDto);

    CompanyDto findCompanyById(Long id);

    CompanyDtoResponse getAllCompanies();

    CompanyDtoResponse getCompaniesByIdCompanies(List<Long> idCompanies);


}
