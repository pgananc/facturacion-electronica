package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IClientService extends ICRUD<Client> {

    void saveClientAndContact(ClientDto clientDto);


    public void updateClientAndContact(ClientDto clientDto, Long idClient);

    void delete(Long id);


    List<ClientDto> findClientByIdAndContact(Long idClient);


    public Page<ClientDto> findClientByIdentificationOrNameOrType(String companyIdentification, String identification, String name
            , Integer clientType, Boolean status, Pageable pageable);
    List<ClientDto> findByCompanyIdentificationAndClientsActive(String companyIdentification);

    boolean existsByCompanyIdentificationAndClientIdentification(String companyIdentification, String identification);


}
