package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.dto.CompanyClientDto;
import com.wallparisoft.ebill.customer.entity.CompanyClient;

public interface ICompanyClientService extends ICRUD<CompanyClient> {
    CompanyClientDto findByIdCompanyAndAndIdClient(Long idCompany, Long idClient);

    void delete(Long idCompany, Long idClient);

    CompanyClient saveCompanyClient(Long idCompany, Long idClient);

    CompanyClientDto getClientsFromACompany(Long idCompany);
}
