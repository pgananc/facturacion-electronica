package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.ContactDto;
import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.ClientContact;
import com.wallparisoft.ebill.customer.entity.Contact;
import com.wallparisoft.ebill.customer.mapper.ClientMapper;
import com.wallparisoft.ebill.customer.mapper.ContactMapper;
import com.wallparisoft.ebill.customer.repository.IClientContactRepo;
import com.wallparisoft.ebill.customer.repository.IClientRepo;
import com.wallparisoft.ebill.customer.repository.IContactRepo;
import com.wallparisoft.ebill.customer.service.IClientService;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ClientServiceImpl implements IClientService {

    @Autowired
    private IClientRepo clientRepo;
    @Autowired
    private IClientContactRepo clientContactRepo;
    @Autowired
    private IContactRepo contactRepo;

    @Autowired
    private ClientMapper clientMapper;
    @Autowired
    private ContactMapper contactMapper;

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
        return this.clientRepo.findById(id).orElseThrow(() -> new ModelNotFoundException("Client not found for this id :: " + id));
    }

    @Override
    public void delete(Long id) {
        Client client = findById(id);
        client.setStatus(false);
        clientRepo.save(client);
    }

    @Override
    public List<ClientDto> findClientsActiveAndContactActive() {
        List<ClientDto> clientDtoList = new ArrayList<>();
        List<Client> clients = this.clientRepo.findClientsActive();
        clients.parallelStream().forEach(x -> {
            ClientDto clientDto = clientMapper.convertClientToClientDto(x);
            List<Contact> contacts = contactRepo.findClientContactActiveByIdClient(x.getIdClient());
            List<ContactDto> contactsDto = contactMapper.convertContactListToContactDtoList(contacts);
            clientDto.setContacts(contactsDto);
            clientDtoList.add(clientDto);
        });
        return clientDtoList;
    }

    @Transactional
    @Override
    public void saveClientAndContact(ClientDto clientDto) {
        Client client = clientMapper.convertClientDtoToClient(clientDto);
        List<Contact> contacts = contactMapper.convertContactDtoListToContactList(clientDto.getContacts());
        Client clientSave = save(client);
        contacts.forEach(x -> {
            Contact contact = contactRepo.save(x);
            ClientContact clientContact = ClientContact.builder()
                    .client(clientSave)
                    .contact(contact)
                    .status(contact.isStatus()).build();
            clientContactRepo.save(clientContact);

        });
    }

    @Transactional
    @Override
    public void updateClientAndContact(ClientDto clientDto, Long idClient) {
        Client clientSave = findById(idClient);
        Client client = clientMapper.convertClientDtoToClient(clientDto);
        List<Contact> contacts = contactMapper.convertContactDtoListToContactList(clientDto.getContacts());

        client.setCreationDate(clientSave.getCreationDate());
        save(client);
        contacts.forEach(x -> {
            Optional<ClientContact> clientContactOptional = clientContactRepo.findByIdClientAndIdContact(client.getIdClient(), x.getIdContact());
            if (clientContactOptional.isPresent()) {
                ClientContact clientContact = clientContactOptional.get();
                clientContact.setStatus(x.isStatus());
                clientContactRepo.save(clientContact);
                x.setCreationDate(clientContact.getCreationDate());
                contactRepo.save(x);
            }
        });
    }
}
