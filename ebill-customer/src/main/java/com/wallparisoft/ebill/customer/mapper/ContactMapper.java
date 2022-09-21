package com.wallparisoft.ebill.customer.mapper;


import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.dto.ContactDto;
import com.wallparisoft.ebill.customer.entity.Company;
import com.wallparisoft.ebill.customer.entity.Contact;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContactMapper {

    ContactMapper INSTANCE = Mappers.getMapper(ContactMapper.class);

    Contact convertContactDtoToContact(ContactDto contactDto);
    List<Contact> convertContactDtoListToContactList(List<ContactDto> list);

    @InheritInverseConfiguration
    ContactDto convertContactToContactDto(Contact contact);
    List<ContactDto> convertContactListToContactDtoList(List<Contact> list);
}
