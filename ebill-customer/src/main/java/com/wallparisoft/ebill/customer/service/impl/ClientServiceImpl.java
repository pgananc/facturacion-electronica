package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.ClientContact;
import com.wallparisoft.ebill.customer.entity.Contact;
import com.wallparisoft.ebill.customer.exception.ModelNotFoundException;
import com.wallparisoft.ebill.customer.repository.IClientRepo;
import com.wallparisoft.ebill.customer.repository.IContactRepo;
import com.wallparisoft.ebill.customer.service.IClientContactService;
import com.wallparisoft.ebill.customer.service.IClientService;

import java.util.List;

import com.wallparisoft.ebill.customer.service.IContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class ClientServiceImpl implements IClientService {

    @Autowired
    private IClientRepo clientRepo;

    @Autowired
    private IContactService contactService;
    @Autowired
    private IClientContactService clientContactService;

    @Override
    public Client save(Client entity) {
        return clientRepo.save(entity);

    }

    @Override
    public Client update(Client entity, Long id) {
        return null;
    }

    @Override
    public List<Client> findAll() {
        return clientRepo.findAll();
    }

    @Override
    public Client findById(Long id) {
        return this.clientRepo.findById(id)
                .orElseThrow(() -> new ModelNotFoundException("Client not found for this id :: " + id));
    }

    @Override
    public void delete(Long id) {
        Client client = findById(id);
        client.setStatus(false);
        clientRepo.save(client);
    }

    @Transactional
    @Override
    public void saveClientAndContact(Client client, List<Contact> contacts) {
        Client clientSave = save(client);
        contacts.forEach(x -> {
            Contact contact = contactService.save(x);
            ClientContact clientContact = new ClientContact();
            clientContact.setClient(clientSave);
            clientContact.setContact(contact);
            clientContact.setStatus(true);
            clientContactService.save(clientContact);
        });
    }
}
