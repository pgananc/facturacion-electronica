package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.dto.CompanyDetailsDto;
import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.entity.Company;
import com.wallparisoft.ebill.customer.response.CompanyDetailDtoResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICompanyService extends ICRUD<Company> {
  void saveCompanyAndContact(CompanyDetailsDto companyDto);

  void updateCompanyAndContact(CompanyDetailsDto companyDetailsDto, Long idCompany);

  void updateCompany(CompanyDto companyDto);

  void delete(Long id);

  List<CompanyDetailsDto> findCompaniesActiveAndContactActive();

  Page<CompanyDetailsDto> findCompanyByIdentificationOrName(
      String identification,
      String name,
      Boolean status,
      List<Long> companiesIds,
      boolean isSuperAdmin,
      Pageable pageable);

  boolean existsByIdentification(String identification);

  CompanyDto saveCompany(CompanyDto companyDto);

  CompanyDetailsDto findCompanyById(Long id);

  CompanyDetailDtoResponse getAllCompanies();

  CompanyDetailDtoResponse getCompaniesByIdCompanies(List<Long> idCompanies);

  String uploadLogo(Long idCompany, org.springframework.web.multipart.MultipartFile file)
      throws java.io.IOException;

  String getLogoPath(Long idCompany);

  void deleteLogo(Long idCompany);

  /**
   * Obtiene empresas activas excluyendo las que tienen IDs en la lista
   *
   * @param excludeIds Lista de IDs a excluir (puede ser null o vacía)
   * @return Lista de empresas que no están en excludeIds
   */
  List<CompanyDetailsDto> getCompaniesExcludingIds(List<Long> excludeIds);
}
