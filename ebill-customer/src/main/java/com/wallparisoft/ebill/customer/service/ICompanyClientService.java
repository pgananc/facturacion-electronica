package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.CompanyClientDto;
import com.wallparisoft.ebill.customer.entity.CompanyClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICompanyClientService extends ICRUD<CompanyClient> {
    CompanyClientDto findByCompanyIdentificationAndIdClient(String companyIdentification, Long idClient);

    void delete(String companyIdentification, Long idClient);

    CompanyClient saveCompanyClient(String companyIdentification, Long idClient);

    Page<ClientDto> getClientsFromACompanyPageable(String companyIdentification, Pageable pageable);

    CompanyClientDto getClientsFromACompany(String companyIdentification);

}
