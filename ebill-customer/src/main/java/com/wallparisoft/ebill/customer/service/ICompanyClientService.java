package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.CompanyClientDto;
import com.wallparisoft.ebill.customer.entity.CompanyClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICompanyClientService extends ICRUD<CompanyClient> {
    CompanyClientDto findByIdCompanyAndIdClient(Long idCompany, Long idClient);

    void delete(Long idCompany, Long idClient);

    CompanyClient saveCompanyClient(Long idCompany, Long idClient);

    Page<ClientDto> getClientsFromACompanyPageable(Long idCompany, Pageable pageable);

    CompanyClientDto getClientsFromACompany(Long idCompany);
}
