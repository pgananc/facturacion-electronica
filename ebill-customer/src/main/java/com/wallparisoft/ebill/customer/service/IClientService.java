package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.entity.Client;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IClientService extends ICRUD<Client> {

  ClientDto saveClientAndContact(ClientDto clientDto);

  public void updateClientAndContact(ClientDto clientDto, Long idClient);

  void delete(Long id);

  List<ClientDto> findClientByIdAndContact(Long idClient);

  public Page<ClientDto> findClientByIdentificationOrNameOrType(
      String companyIdentification,
      String identification,
      String name,
      Integer clientType,
      Boolean status,
      String email,
      String phone,
      String mobile,
      String address,
      Pageable pageable);

  List<ClientDto> findByCompanyIdentificationAndClientsActive(String companyIdentification);

  ClientDto findByIdentificationAndEmail(String identification, String email);

  List<ClientDto> findByIdentification(String identification);

  List<ClientDto> findByIdentificationList(String identification);
}
