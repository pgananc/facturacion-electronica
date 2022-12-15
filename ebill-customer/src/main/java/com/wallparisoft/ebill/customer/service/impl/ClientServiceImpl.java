package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.ContactDto;
import com.wallparisoft.ebill.customer.entity.*;
import com.wallparisoft.ebill.customer.mapper.ClientMapper;
import com.wallparisoft.ebill.customer.mapper.ContactMapper;
import com.wallparisoft.ebill.customer.repository.IClientContactRepo;
import com.wallparisoft.ebill.customer.repository.IClientRepo;
import com.wallparisoft.ebill.customer.repository.ICompanyClientRepo;
import com.wallparisoft.ebill.customer.repository.IContactRepo;
import com.wallparisoft.ebill.customer.service.IClientService;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    @Autowired
    private ICompanyClientRepo companyClientRepo;

    @PersistenceContext
    private EntityManager entityManager;

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
    public List<ClientDto> findByCompanyIdentificationAndClientsActive(String companyIdentification) {
        List<ClientDto> clientDtoList = new ArrayList<>();
        Optional<List<Client>> clientList = this.clientRepo.findByCompanyIdentificationAndClientStatusOrderByClientAsc(companyIdentification,true);
        if(clientList.isPresent()){
            clientList.get().parallelStream().forEach(client -> {
                ClientDto clientDto = clientMapper.convertClientToClientDto(client);
                clientDtoList.add(clientDto);
            });
        }
        return clientDtoList;
    }

    @Override
    public boolean existsByCompanyIdentificationAndClientIdentification(String companyIdentification, String identification) {
        return companyClientRepo.existsByCompanyIdentificationAndClientIdentification(companyIdentification,identification);
    }

    @Override
    public List<ClientDto> findClientByIdAndContact(Long idClient) {
        List<ClientDto> clientDtoList = new ArrayList<>();
        List<Client> clients = this.clientRepo.findByIdClientOrderByIdentification(idClient);
        clients.parallelStream().forEach(client -> {
            ClientDto clientDto = clientMapper.convertClientToClientDto(client);
             Optional<CompanyClient> companyClient = companyClientRepo.findByClient_IdClient(idClient);
             if(companyClient.isPresent()){
                 clientDto.setCompanyIdentification(companyClient.get().getCompanyIdentification());
             }
            List<Contact> contacts = contactRepo.findClientContactActiveByIdClient(client.getIdClient());
            List<ContactDto> contactsDto = contactMapper.convertContactListToContactDtoList(contacts);
            clientDto.setContacts(contactsDto);
            clientDtoList.add(clientDto);
        });
        return clientDtoList;
    }

    @Override
    public Page<ClientDto> findClientByIdentificationOrNameOrType(String companyIdentification, String identification, String name
            , Integer clientType, Boolean status, Pageable pageable) {
        Page<Client> clients = clientRepo
                .findClientByCompanyAndIdentificationOrNameOrType(companyIdentification,"%"+identification+"%", "%"+name.toUpperCase()+"%", clientType, status, pageable);
        List<ClientDto> clientDtoList = new ArrayList<>();
        clients.getContent().parallelStream().forEach(client -> {
            ClientDto clientDto = clientMapper.convertClientToClientDto(client);
            clientDtoList.add(clientDto);
        });
        return new PageImpl<>(clientDtoList, pageable, clients.getTotalElements());
    }


    public Long countClientByIdentificationOrNameOrType(Query queryCount) {
        return (Long) queryCount.getSingleResult();

    }

    @Transactional
    @Override
    public void saveClientAndContact(ClientDto clientDto) {
        Client client = clientMapper.convertClientDtoToClient(clientDto);
        List<Contact> contacts = contactMapper.convertContactDtoListToContactList(clientDto.getContacts());
        Client clientSave = save(client);
        contacts.forEach(contact -> {
            Contact contactSave = contactRepo.save(contact);
            ClientContact clientContact = ClientContact.builder()
                    .client(clientSave)
                    .contact(contactSave)
                    .status(contactSave.getStatus()).build();
            clientContactRepo.save(clientContact);

        });
        CompanyClient companyClient= CompanyClient
                                    .builder()
                                    .client(clientSave)
                                    .companyIdentification(clientDto.getCompanyIdentification())
                                    .build();
        companyClientRepo.save(companyClient);
    }

    @Transactional
    @Override
    public void updateClientAndContact(ClientDto clientDto, Long idClient) {
        Client clientSave = findById(idClient);
        Client client = clientMapper.convertClientDtoToClient(clientDto);
        List<Contact> contacts = contactMapper.convertContactDtoListToContactList(clientDto.getContacts());

        client.setCreationDate(clientSave.getCreationDate());
        save(client);
        contacts.forEach(contact -> {
            Optional<ClientContact> clientContactOptional = clientContactRepo.findByIdClientAndIdContact(client.getIdClient(), contact.getIdContact());
            if (clientContactOptional.isPresent()) {
                ClientContact clientContact = clientContactOptional.get();
                clientContact.setStatus(contact.getStatus());
                clientContactRepo.save(clientContact);
                contact.setCreationDate(clientContact.getCreationDate());
                contactRepo.save(contact);
            } else if (contact.getIdContact() == 0) {
                Contact contactSave = contactRepo.save(contact);
                ClientContact clientContact = ClientContact.builder()
                        .client(clientSave)
                        .contact(contactSave)
                        .status(contactSave.getStatus()).build();
                clientContactRepo.save(clientContact);

            }
        });
    }


}
