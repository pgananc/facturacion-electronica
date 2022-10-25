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
import org.springframework.beans.support.PagedListHolder;
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

import static java.util.Objects.*;

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
    public List<ClientDto> findClientsActive(Pageable pageable) {
        return null;
    }

    @Override
    public List<ClientDto> findClientsActive() {
        List<ClientDto> clientDtoList = new ArrayList<>();
        List<Client> clients = this.clientRepo.findClientsActive();
        clients.parallelStream().forEach(x -> {
            ClientDto clientDto = clientMapper.convertClientToClientDto(x);
            clientDtoList.add(clientDto);
        });
     return clientDtoList;
    }

    @Override
    public boolean existsByIdentification(String identification) {
        return clientRepo.existsByIdentification(identification);
    }

    @Override
    public List<ClientDto> findClientByIdActiveAndContactActive(Long idClient) {
        List<ClientDto> clientDtoList = new ArrayList<>();
        List<Client> clients = this.clientRepo.findClientActiveById(idClient);
        clients.parallelStream().forEach(x -> {
            ClientDto clientDto = clientMapper.convertClientToClientDto(x);
            List<Contact> contacts = contactRepo.findClientContactActiveByIdClient(x.getIdClient());
            List<ContactDto> contactsDto = contactMapper.convertContactListToContactDtoList(contacts);
            clientDto.setContacts(contactsDto);
            clientDtoList.add(clientDto);
        });
        return clientDtoList;
    }

    @Override
    public Page<ClientDto> findClientByIdentificationOrNameOrType(String identification, String name
            , Integer clientType, Boolean status, Pageable pageable) {
        StringBuilder sql = new StringBuilder();
        StringBuilder sqlCount = new StringBuilder();
        sql.append("SELECT c FROM Client  c WHERE 1=1");
        sqlCount.append("SELECT count(c) FROM Client  c WHERE 1=1");
        if (nonNull(identification) && !identification.isEmpty()) {
            sql.append(" and c.identification like :identification");
            sqlCount.append(" and c.identification like :identification");
        }
        if (nonNull(name) && !name.isEmpty()) {
            sql.append(" and upper(c.name) like :name");
            sqlCount.append(" and upper(c.name) like :name");
        }
        if (nonNull(clientType) && clientType > 0) {
            sql.append(" and c.clientType = :clientType");
            sqlCount.append(" and c.clientType = :clientType");
        }
        if (nonNull(status)) {
            sql.append(" and c.status = :status");
            sqlCount.append(" and c.status = :status");
        }

        Query query = entityManager.createQuery(sql.toString());
        Query queryCount = entityManager.createQuery(sqlCount.toString());
        if (nonNull(identification) && !identification.isEmpty()) {
            query.setParameter("identification", "%" + identification + "%");
            queryCount.setParameter("identification", "%" + identification + "%");
        }
        if (nonNull(name) && !name.isEmpty()) {
            query.setParameter("name", "%" + name.toUpperCase() + "%");
            queryCount.setParameter("name", "%" + name.toUpperCase() + "%");
        }
        if (nonNull(clientType) && clientType > 0) {
            query.setParameter("clientType", clientType);
            queryCount.setParameter("clientType", clientType);
        }
        if (nonNull(status)) {
            query.setParameter("status", status);
            queryCount.setParameter("status", status);
        }

        long currentTotal = countClientByIdentificationOrNameOrType(queryCount);

        query.setFirstResult((pageable.getPageNumber()) * pageable.getPageSize());
        query.setMaxResults(pageable.getPageSize());

        List<Client> clients = query.getResultList();
        List<ClientDto> clientDtoList = new ArrayList<>();
        clients.parallelStream().forEach(x -> {
            ClientDto clientDto = clientMapper.convertClientToClientDto(x);
            clientDtoList.add(clientDto);
        });

        return new PageImpl<>(clientDtoList, pageable, currentTotal);
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
