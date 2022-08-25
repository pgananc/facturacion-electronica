package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.Contact;

import javax.transaction.Transactional;
import java.util.List;

public interface IClientService extends ICRUD<Client> {

    void saveClientAndContact(Client client, List<Contact> contacts);


    public void updateClientAndContact(Client client, List<Contact> contacts);

    void delete(Long id);

    List<ClientDto> findClientsActiveAndContactActive();



}
