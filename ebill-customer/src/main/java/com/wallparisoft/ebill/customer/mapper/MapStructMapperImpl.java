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

        Client client = new Client();
        client.setClientType(clientDto.getClientType());
        client.setName(clientDto.getName());
        client.setIdentification(clientDto.getIdentification());
        client.setStatus(clientDto.getStatus());
        client.setIdType(clientDto.getIdType());
        client.setIdCompany(clientDto.getIdCompany());
        return client;
    }

    @Override
    public Contact convertContactDtoToContact(ContactDto contactDto) {
        if (contactDto == null) {
            return null;
        }
        Contact contact = new Contact();
        contact.setContactType(contactDto.getContactType());
        contact.setStatus(contactDto.getStatus());
        contact.setValue(contactDto.getValue());
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
}
