package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.ContactDto;
import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.Contact;
import com.wallparisoft.ebill.customer.response.ClientDtoResponse;

import javax.transaction.Transactional;
import java.util.List;

public interface IClientService extends ICRUD<Client> {

    void saveClientAndContact(ClientDto clientDto);


    public void updateClientAndContact(ClientDto clientDto);

    void delete(Long id);

    List<ClientDto> findClientsActiveAndContactActive();



}
