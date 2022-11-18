package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ICompanyService extends ICRUD<Company> {
    void saveCompanyAndContact(CompanyDto companyDto);


    public void updateCompanyAndContact(CompanyDto companyDto, Long idCompany);

    void delete(Long id);

    List<CompanyDto> findCompaniesActiveAndContactActive();

    Page<CompanyDto> findCompanyByIdentificationOrNameOrBranchOfficeCode(String identification, String name, String branchOfficeCode, Boolean status, Pageable pageable);

    boolean existsByIdentification(String identification);
}
