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

    List<ClientDto> findClientsActive(Pageable pageable);

    List<ClientDto> findClientByIdActiveAndContactActive(Long idClient);


    Page<ClientDto> findClientByIdentificationOrNameOrType(String identification, String name, Integer clientType, Boolean status, Pageable pageable);

    List<ClientDto> findClientsActive();

}
