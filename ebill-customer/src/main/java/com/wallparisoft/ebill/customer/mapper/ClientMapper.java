package com.wallparisoft.ebill.customer.mapper;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.entity.Client;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClientMapper {

  @Mapping(target = "status", source = "status")
  Client convertClientDtoToClient(ClientDto clientDto);

  @InheritInverseConfiguration
  ClientDto convertClientToClientDto(Client client);
}
