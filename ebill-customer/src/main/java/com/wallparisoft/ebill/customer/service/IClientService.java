package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.Contact;

import java.util.List;

public interface IClientService extends ICRUD<Client> {

    public void saveClientAndContact(Client client, List<Contact> contacts);

    void delete(Long id);
}
