package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.ContactDto;
import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.ClientContact;
import com.wallparisoft.ebill.customer.entity.CompanyClient;
import com.wallparisoft.ebill.customer.entity.Contact;
import com.wallparisoft.ebill.customer.mapper.ClientMapper;
import com.wallparisoft.ebill.customer.mapper.ContactMapper;
import com.wallparisoft.ebill.customer.repository.IClientContactRepo;
import com.wallparisoft.ebill.customer.repository.IClientRepo;
import com.wallparisoft.ebill.customer.repository.ICompanyClientRepo;
import com.wallparisoft.ebill.customer.repository.IContactRepo;
import com.wallparisoft.ebill.customer.service.IClientService;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class ClientServiceImpl implements IClientService {

  private final IClientRepo clientRepo;

  private final IClientContactRepo clientContactRepo;

  private final IContactRepo contactRepo;

  private final ClientMapper clientMapper;

  private final ContactMapper contactMapper;

  private final ICompanyClientRepo companyClientRepo;

  @Override
  public Client save(Client entity) {
    return clientRepo.save(entity);
  }

  @Override
  public Client update(Client entity, Long id) {
    Client existingClient = findById(id);
    existingClient.setIdType(entity.getIdType());
    existingClient.setIdentification(entity.getIdentification());
    existingClient.setName(entity.getName());
    existingClient.setClientType(entity.getClientType());
    existingClient.setStatus(entity.getStatus());
    return clientRepo.save(existingClient);
  }

  @Override
  public List<Client> findAll() {

    return clientRepo.findAll();
  }

  @Override
  public Client findById(Long id) {
    return this.clientRepo
        .findById(id)
        .orElseThrow(() -> new ModelNotFoundException("Client not found for this id :: " + id));
  }

  @Override
  @CacheEvict(
      value = {"clients", "client-detail"},
      allEntries = true)
  public void delete(Long id) {
    Client client = findById(id);
    client.setStatus(false);
    clientRepo.save(client);
  }

  @Override
  @Cacheable(value = "clients", key = "#companyIdentification", sync = true)
  public List<ClientDto> findByCompanyIdentificationAndClientsActive(String companyIdentification) {
    Optional<List<Client>> clientList =
        this.clientRepo.findByCompanyIdentificationAndClientStatusOrderByClientAsc(
            companyIdentification, true);

    return clientList
        .map(
            clients ->
                clients.stream()
                    .map(
                        client -> {
                          ClientDto clientDto = clientMapper.convertClientToClientDto(client);
                          clientDto.setContacts(findContactsByIdClient(client.getIdClient()));
                          return clientDto;
                        })
                    .collect(Collectors.toList()))
        .orElse(new ArrayList<>());
  }

  @Override
  @Cacheable(value = "client-detail", key = "#idClient", sync = true)
  public List<ClientDto> findClientByIdAndContact(Long idClient) {
    List<Client> clients = this.clientRepo.findByIdClientOrderByIdentification(idClient);

    return clients.stream()
        .map(
            client -> {
              ClientDto clientDto = clientMapper.convertClientToClientDto(client);
              Optional<CompanyClient> companyClient =
                  companyClientRepo.findByClient_IdClient(idClient);
              companyClient.ifPresent(
                  value -> clientDto.setCompanyIdentification(value.getCompanyIdentification()));
              clientDto.setContacts(findContactsByIdClient(client.getIdClient()));
              return clientDto;
            })
        .collect(Collectors.toList());
  }

  @Override
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
      Pageable pageable) {
    // Hacer null-safe los parámetros de búsqueda
    String safeIdentification =
        identification != null && !identification.isEmpty() ? "%" + identification + "%" : "%%";
    String safeName = name != null && !name.isEmpty() ? "%" + name.toUpperCase() + "%" : "%%";
    String safeEmail = email != null && !email.isEmpty() ? "%" + email + "%" : null;
    String safePhone = phone != null && !phone.isEmpty() ? "%" + phone + "%" : null;
    String safeMobile = mobile != null && !mobile.isEmpty() ? "%" + mobile + "%" : null;
    String safeAddress =
        address != null && !address.isEmpty() ? "%" + address.toUpperCase() + "%" : null;

    Page<Client> clients =
        clientRepo.findClientByCompanyAndIdentificationOrNameOrType(
            companyIdentification,
            safeIdentification,
            safeName,
            clientType,
            status,
            safeEmail,
            safePhone,
            safeMobile,
            safeAddress,
            pageable);

    // ========== SOLUCIÓN N+1: BATCH LOADING ==========
    // En lugar de hacer 1 query por cada cliente, hacemos 1 query para TODOS

    // 1. Extraer IDs de todos los clientes de la página
    List<Long> clientIds =
        clients.getContent().stream().map(Client::getIdClient).collect(Collectors.toList());

    // 2. Si hay clientes, cargar TODOS sus contactos en UNA sola query
    Map<Long, List<ContactDto>> contactsByClientId = Map.of(); // Default vacío

    if (!clientIds.isEmpty()) {
      // Query única: SELECT * FROM contact WHERE id_client IN (1,2,3,4,5...)
      List<com.wallparisoft.ebill.customer.dto.ClientContactPairDto> pairs =
          contactRepo.findClientContactsActiveByClientIds(clientIds);

      // 3. Agrupar contactos por cliente en memoria (sin queries adicionales)
      contactsByClientId =
          pairs.stream()
              .collect(
                  Collectors.groupingBy(
                      com.wallparisoft.ebill.customer.dto.ClientContactPairDto::getClientId,
                      Collectors.mapping(
                          pair -> contactMapper.convertContactToContactDto(pair.getContact()),
                          Collectors.toList())));
    }

    // 4. Mapear clientes y asignar sus contactos (lookup en memoria, 0 queries)
    final Map<Long, List<ContactDto>> finalContactsMap = contactsByClientId;
    List<ClientDto> clientDtoList =
        clients.getContent().stream()
            .map(
                client -> {
                  ClientDto clientDto = clientMapper.convertClientToClientDto(client);
                  // Asignar contactos desde el mapa (O(1) lookup, sin query)
                  clientDto.setContacts(
                      finalContactsMap.getOrDefault(client.getIdClient(), List.of()));
                  return clientDto;
                })
            .collect(Collectors.toList());

    return new PageImpl<>(clientDtoList, pageable, clients.getTotalElements());
  }

  public Long countClientByIdentificationOrNameOrType(Query queryCount) {
    return (Long) queryCount.getSingleResult();
  }

  @Transactional
  @Override
  @CacheEvict(
      value = {"clients", "client-detail"},
      allEntries = true)
  public ClientDto saveClientAndContact(ClientDto clientDto) {
    // 1. Buscar si ya existe un cliente con esta identificación en esta compañía
    Optional<CompanyClient> existingCompanyClient =
        companyClientRepo.findByCompanyIdentificationAndClientIdentification(
            clientDto.getCompanyIdentification(), clientDto.getIdentification());

    Client clientToSave;
    boolean isReactivation = false;

    if (existingCompanyClient.isPresent()) {
      Client existingClient = existingCompanyClient.get().getClient();

      // Validar si el cliente está activo
      if (existingClient.getStatus()) {
        // CASO A: Cliente existe y está ACTIVO → ERROR (no se puede duplicar)
        log.warn(
            "Intento de crear cliente duplicado con identificación {} (ya existe activo con ID {})",
            clientDto.getIdentification(),
            existingClient.getIdClient());
        throw new IllegalStateException(
            "Cliente ya existe con número de identificación " + clientDto.getIdentification());
      }

      // CASO B: Cliente existe pero está INACTIVO → REACTIVAR y ACTUALIZAR
      log.info(
          "Cliente con identificación {} existe pero está inactivo. Reactivando...",
          clientDto.getIdentification());

      clientToSave = existingClient;
      isReactivation = true;

      // Actualizar datos del cliente existente
      clientToSave.setIdType(clientDto.getIdType());
      clientToSave.setIdentification(clientDto.getIdentification());
      clientToSave.setName(clientDto.getName());
      clientToSave.setClientType(clientDto.getClientType());
      clientToSave.setStatus(true); // Reactivar

      log.info("Cliente con ID {} será reactivado", clientToSave.getIdClient());
    } else {
      // CASO C: Cliente NO existe → CREAR NUEVO
      log.info(
          "Cliente con identificación {} no existe. Creando nuevo registro...",
          clientDto.getIdentification());

      clientToSave = clientMapper.convertClientDtoToClient(clientDto);
      clientToSave.setStatus(true);
    }

    // Guardar el cliente (INSERT si es nuevo, UPDATE si existe)
    Client clientSaved = save(clientToSave);

    // 2. Procesar contactos (actualizar existentes o crear nuevos)
    processClientContacts(clientSaved, clientDto.getContacts());

    // 3. Crear o actualizar la relación CompanyClient
    if (!existingCompanyClient.isPresent()) {
      // Solo crear CompanyClient si no existía antes
      CompanyClient companyClient =
          CompanyClient.builder()
              .client(clientSaved)
              .companyIdentification(clientDto.getCompanyIdentification())
              .build();
      companyClientRepo.save(companyClient);
      log.info(
          "Relación CompanyClient creada para cliente ID {} y compañía {}",
          clientSaved.getIdClient(),
          clientDto.getCompanyIdentification());
    }

    log.info(
        "Cliente {} {} exitosamente. ID: {}",
        isReactivation ? "reactivado" : "creado",
        clientDto.getIdentification(),
        clientSaved.getIdClient());

    // 4. Retornar el DTO completo con los datos actualizados
    ClientDto savedClientDto = clientMapper.convertClientToClientDto(clientSaved);
    savedClientDto.setCompanyIdentification(clientDto.getCompanyIdentification());
    savedClientDto.setContacts(findContactsByIdClient(clientSaved.getIdClient()));

    return savedClientDto;
  }

  /**
   * Procesa los contactos de un cliente: - Si el contacto tiene idContact → actualizar existente -
   * Si el contacto NO tiene idContact pero existe uno con mismo tipo y valor → actualizar - Si es
   * un contacto completamente nuevo → crear
   *
   * @param client Cliente al que pertenecen los contactos
   * @param contactDtos Lista de contactos del DTO
   */
  private void processClientContacts(Client client, List<ContactDto> contactDtos) {
    // Obtener todos los contactos existentes del cliente (activos e inactivos)
    List<Contact> existingContacts =
        contactRepo.findAllClientContactsByIdClient(client.getIdClient());

    contactDtos.forEach(
        contactDto -> {
          Optional<Contact> existingContact = Optional.empty();

          // 1. Si el contacto tiene ID, buscar por ID primero
          if (contactDto.getIdContact() != null) {
            existingContact =
                existingContacts.stream()
                    .filter(ec -> ec.getIdContact().equals(contactDto.getIdContact()))
                    .findFirst();
          }

          // 2. Si no se encontró por ID, buscar por tipo y valor (para evitar duplicados)
          if (!existingContact.isPresent()) {
            existingContact =
                existingContacts.stream()
                    .filter(
                        ec ->
                            ec.getContactType().equals(contactDto.getContactType())
                                && ec.getValue().equals(contactDto.getValue()))
                    .findFirst();
          }

          if (existingContact.isPresent()) {
            // ACTUALIZAR contacto existente
            Contact contactToUpdate = existingContact.get();
            contactToUpdate.setValue(contactDto.getValue());
            contactToUpdate.setContactType(contactDto.getContactType());
            contactToUpdate.setStatus(
                contactDto.getStatus() != null ? contactDto.getStatus() : true);
            contactRepo.save(contactToUpdate);

            log.debug(
                "Contacto actualizado: ID={}, tipo={}, valor={}",
                contactToUpdate.getIdContact(),
                contactToUpdate.getContactType(),
                contactToUpdate.getValue());

            // Actualizar la relación ClientContact si cambió el status
            Optional<ClientContact> clientContactOptional =
                clientContactRepo.findByIdClientAndIdContact(
                    client.getIdClient(), contactToUpdate.getIdContact());
            if (clientContactOptional.isPresent()) {
              ClientContact clientContact = clientContactOptional.get();
              clientContact.setStatus(
                  contactDto.getStatus() != null ? contactDto.getStatus() : true);
              clientContactRepo.save(clientContact);
            }
          } else {
            // CREAR nuevo contacto
            Contact newContact = contactMapper.convertContactDtoToContact(contactDto);
            newContact.setStatus(contactDto.getStatus() != null ? contactDto.getStatus() : true);
            Contact contactSaved = contactRepo.save(newContact);

            log.debug(
                "Contacto nuevo creado: ID={}, tipo={}, valor={}",
                contactSaved.getIdContact(),
                contactSaved.getContactType(),
                contactSaved.getValue());

            // Crear relación ClientContact
            ClientContact clientContact =
                ClientContact.builder()
                    .client(client)
                    .contact(contactSaved)
                    .status(contactDto.getStatus() != null ? contactDto.getStatus() : true)
                    .build();
            clientContactRepo.save(clientContact);
          }
        });
  }

  @Transactional
  @Override
  @CacheEvict(
      value = {"clients", "client-detail"},
      allEntries = true)
  public void updateClientAndContact(ClientDto clientDto, Long idClient) {
    Client clientSave = findById(idClient);

    // Actualizar los campos del cliente existente en lugar de crear uno nuevo
    clientSave.setIdType(clientDto.getIdType());
    clientSave.setIdentification(clientDto.getIdentification());
    clientSave.setName(clientDto.getName());
    clientSave.setClientType(clientDto.getClientType());
    clientSave.setStatus(clientDto.getStatus());

    save(clientSave);

    // Actualizar CompanyClient si cambió el companyIdentification
    Optional<CompanyClient> companyClientOptional =
        companyClientRepo.findByClient_IdClient(idClient);
    if (companyClientOptional.isPresent()) {
      CompanyClient companyClient = companyClientOptional.get();
      if (!companyClient.getCompanyIdentification().equals(clientDto.getCompanyIdentification())) {
        companyClient.setCompanyIdentification(clientDto.getCompanyIdentification());
        companyClientRepo.save(companyClient);
      }
    }

    // Actualizar contactos
    // Obtener todos los contactos existentes del cliente (activos e inactivos)
    List<Contact> existingContacts = contactRepo.findAllClientContactsByIdClient(idClient);

    // Procesar los contactos que vienen del frontend
    clientDto
        .getContacts()
        .forEach(
            contactDto -> {
              Optional<Contact> existingContact = Optional.empty();

              // Si el contacto tiene ID, buscar por ID primero
              if (contactDto.getIdContact() != null) {
                existingContact =
                    existingContacts.stream()
                        .filter(ec -> ec.getIdContact().equals(contactDto.getIdContact()))
                        .findFirst();
              }

              // Si no se encontró por ID, buscar por tipo y valor
              if (!existingContact.isPresent()) {
                existingContact =
                    existingContacts.stream()
                        .filter(
                            ec ->
                                ec.getContactType().equals(contactDto.getContactType())
                                    && ec.getValue().equals(contactDto.getValue()))
                        .findFirst();
              }

              if (existingContact.isPresent()) {
                // Actualizar contacto existente
                Contact contactToUpdate = existingContact.get();
                contactToUpdate.setValue(contactDto.getValue());
                contactToUpdate.setContactType(contactDto.getContactType());
                contactToUpdate.setStatus(contactDto.getStatus());
                contactRepo.save(contactToUpdate);

                // Actualizar la relación ClientContact si cambió el status
                Optional<ClientContact> clientContactOptional =
                    clientContactRepo.findByIdClientAndIdContact(
                        clientSave.getIdClient(), contactToUpdate.getIdContact());
                if (clientContactOptional.isPresent()) {
                  ClientContact clientContact = clientContactOptional.get();
                  clientContact.setStatus(contactDto.getStatus());
                  clientContactRepo.save(clientContact);
                }
              } else {
                // Crear nuevo contacto
                Contact newContact = contactMapper.convertContactDtoToContact(contactDto);
                newContact.setStatus(contactDto.getStatus());
                Contact contactSave = contactRepo.save(newContact);
                ClientContact clientContact =
                    ClientContact.builder()
                        .client(clientSave)
                        .contact(contactSave)
                        .status(contactDto.getStatus())
                        .build();
                clientContactRepo.save(clientContact);
              }
            });
  }

  @Override
  @Cacheable(value = "client-detail", key = "#identification + '_' + #email", sync = true)
  public ClientDto findByIdentificationAndEmail(String identification, String email) {
    // Log the input parameters for debugging
    log.info(
        "Searching for client with identification: '{}' and email: '{}'", identification, email);

    // First, try to find the client by identification only
    Client client = null;
    Optional<List<Client>> listClients = clientRepo.findByIdentificationList(identification);
    if (listClients.isEmpty()) {
      log.warn("No client found with identification: '{}'", identification);
      throw new ModelNotFoundException("Número de identificación no registrada ");
    } else {
      client = listClients.get().get(0);
    }

    if (Objects.isNull(client)) {
      log.warn("No client found with identification: '{}'", identification);
      throw new ModelNotFoundException("Número de identificación no registrada ");
    }

    //    Client client = clientByIdOptional.get();
    log.info(
        "Found client with ID: {}, name: '{}', status: {}",
        client.getIdClient(),
        client.getName(),
        client.getStatus());

    // Now check if this client has the specified email
    List<Contact> emailContacts =
        clientRepo.findContactByClientIdAndEmail(client.getIdClient(), email);

    if (emailContacts.isEmpty()) {
      log.warn(
          "Client with ID {} found, but no email '{}' found in contacts",
          client.getIdClient(),
          email);

      // Log all email contacts for this client for debugging
      List<Contact> allEmailContacts =
          contactRepo.findClientContactActiveByIdClient(client.getIdClient()).stream()
              .filter(contact -> "MAI".equals(contact.getContactType()))
              .toList();

      log.info(
          "Available email contacts for client {}: {}",
          client.getIdClient(),
          allEmailContacts.stream().map(Contact::getValue).toList());

      throw new ModelNotFoundException("No existe el email ingresado.");
    }

    log.info(
        "Found {} matching email contact(s) for client {}",
        emailContacts.size(),
        client.getIdClient());

    ClientDto clientDto = clientMapper.convertClientToClientDto(client);

    // Get company identification from CompanyClient relationship
    Optional<CompanyClient> companyClient =
        companyClientRepo.findByClient_IdClient(client.getIdClient());
    companyClient.ifPresent(
        value -> clientDto.setCompanyIdentification(value.getCompanyIdentification()));

    clientDto.setContacts(findContactsByIdClient(client.getIdClient()));

    return clientDto;
  }

  List<ContactDto> findContactsByIdClient(Long idClient) {
    List<Contact> contacts = contactRepo.findClientContactActiveByIdClient(idClient);
    return contactMapper.convertContactListToContactDtoList(contacts);
  }

  @Override
  public List<ClientDto> findByIdentification(String identification) {
    Optional<Client> clientOpt = clientRepo.findByIdentification(identification);
    if (clientOpt.isEmpty()) {
      return List.of();
    }
    Client client = clientOpt.get();
    ClientDto clientDto = clientMapper.convertClientToClientDto(client);
    return List.of(clientDto);
  }

  @Override
  public List<ClientDto> findByIdentificationList(String identification) {
    Optional<List<Client>> clientOpt = clientRepo.findByIdentificationList(identification);
    if (clientOpt.isEmpty()) {
      return List.of();
    }
    List<ClientDto> listClientDtos = new ArrayList<>();
    for (Client client : clientOpt.get()) {
      ClientDto clientDto = clientMapper.convertClientToClientDto(client);
      listClientDtos.add(clientDto);
    }

    return listClientDtos;
  }
}
