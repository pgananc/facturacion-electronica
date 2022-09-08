package com.wallparisoft.ebill.customer.mapper;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.ContactDto;
import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.Contact;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MapStructMapper {
    Client convertClientDtoToClient(ClientDto clientDto);

    Contact convertContactDtoToContact(ContactDto contactDto);

    List<Contact> convertContactDtoListToContactList(List<ContactDto> list);

    ClientDto convertClientToClientDto(Client client);

    ContactDto convertContactToContactDto(Contact contact);
    List<ContactDto> convertContactListToContactDtoList(List<Contact> list);
}
