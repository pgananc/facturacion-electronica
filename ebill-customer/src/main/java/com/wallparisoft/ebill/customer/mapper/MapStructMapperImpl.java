package com.wallparisoft.ebill.customer.mapper;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.ContactDto;
import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.Contact;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MapStructMapperImpl implements MapStructMapper {
    @Override
    public Client convertClientDtoToClient(ClientDto clientDto) {
        if (clientDto == null) {
            return null;
        }

        Client client = Client.builder().idClient(clientDto.getIdClient()).
                clientType(clientDto.getClientType()).
                name(clientDto.getName()).
                identification(clientDto.getIdentification()).
                status(clientDto.getStatus()).
                idType(clientDto.getIdType()).
                idCompany(clientDto.getIdCompany()).
                build();
        return client;
    }

    @Override
    public Contact convertContactDtoToContact(ContactDto contactDto) {
        if (contactDto == null) {
            return null;
        }
        Contact contact = Contact.builder()
                .idContact(contactDto.getIdContact()).
                contactType(contactDto.getContactType()).
                status(contactDto.getStatus()).
                value(contactDto.getValue()).build();
        return contact;
    }

    @Override
    public List<Contact> convertContactDtoListToContactList(List<ContactDto> list) {
        List<Contact> contacts = new ArrayList<>();
        list.forEach(x -> {
            contacts.add(convertContactDtoToContact(x));
        });
        return contacts;
    }

    @Override
    public ClientDto convertClientToClientDto(Client client) {
        if (client == null) {
            return null;
        }

        ClientDto clientDto = new ClientDto();
        clientDto.setIdClient(client.getIdClient());
        clientDto.setClientType(client.getClientType());
        clientDto.setName(client.getName());
        clientDto.setIdentification(client.getIdentification());
        clientDto.setStatus(client.isStatus());
        clientDto.setIdType(client.getIdType());
        clientDto.setIdCompany(client.getIdCompany());
        return clientDto;
    }

    @Override
    public ContactDto convertContactToContactDto(Contact contact) {
        if (contact == null) {
            return null;
        }
        ContactDto contactDto = new ContactDto();
        contactDto.setIdContact(contact.getIdContact());
        contactDto.setContactType(contact.getContactType());
        contactDto.setStatus(contact.isStatus());
        contactDto.setValue(contact.getValue());
        return contactDto;
    }

    @Override
    public List<ContactDto> convertContactListToContactDtoList(List<Contact> list) {

        List<ContactDto> contactsDto = new ArrayList<>();
        list.forEach(x -> {
            contactsDto.add(convertContactToContactDto(x));
        });
        return contactsDto;
    }
}
