package com.wallparisoft.ebill.customer.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.ClientRequestDto;
import com.wallparisoft.ebill.customer.dto.ClientSearchRequestDto;
import com.wallparisoft.ebill.customer.response.ClientDtoResponse;
import com.wallparisoft.ebill.customer.service.IClientService;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/client")
@FieldDefaults(level = PRIVATE)
@Slf4j
public class ClientController {

  final IClientService clientService;

  @PostMapping
  public ResponseEntity<ClientDto> save(@Valid @RequestBody ClientDto clientDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    ClientDto savedClient = clientService.saveClientAndContact(clientDto);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(savedClient),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(savedClient, HttpStatus.CREATED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BasicResponse> delete(@PathVariable("id") Long id) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(id),
        REQUEST,
        LEVEL_001);
    clientService.delete(id);
    BasicResponse response = getBasicResponse();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getMessage()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/company/{companyIdentification}")
  public ResponseEntity<ClientDtoResponse> findByCompanyIdentificationAndClientsActive(
      @PathVariable("companyIdentification") String companyIdentification) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    List<ClientDto> clients =
        clientService.findByCompanyIdentificationAndClientsActive(companyIdentification);
    ClientDtoResponse response =
        ClientDtoResponse.builder()
            .status(HttpStatus.OK.getReasonPhrase())
            .clientDtos(clients)
            .build();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getClientDtos()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/{idClient}")
  public ResponseEntity<ClientDtoResponse> findClientAndContact(
      @PathVariable(value = "idClient") Long idClient) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    List<ClientDto> clients = clientService.findClientByIdAndContact(idClient);
    ClientDtoResponse response =
        ClientDtoResponse.builder()
            .status(HttpStatus.OK.getReasonPhrase())
            .clientDtos(clients)
            .build();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getClientDtos()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @PatchMapping("/{idClient}")
  public ResponseEntity<BasicResponse> update(
      @Valid @RequestBody ClientDto clientDto, @PathVariable Long idClient) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(
                "ID client: "
                    .concat(idClient.toString())
                    .concat(", Object company: ")
                    .concat(clientDto.toString())),
        REQUEST,
        LEVEL_001);
    clientService.updateClientAndContact(clientDto, idClient);
    BasicResponse response = getBasicResponse();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getMessage()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @PostMapping("/pageable")
  public ResponseEntity<Page<ClientDto>> listPageable(
      Pageable pageable, @Valid @RequestBody ClientRequestDto clientRequestDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];

    // Valores por defecto si el DTO es null o tiene campos null
    String companyId = clientRequestDto.getCompanyIdentification();
    String identification =
        clientRequestDto.getIdentification() != null ? clientRequestDto.getIdentification() : "";
    String name = clientRequestDto.getName() != null ? clientRequestDto.getName() : "";
    // Si clientType es 0, tratarlo como null (sin filtro)
    Integer clientType =
        clientRequestDto.getClientType() != null && clientRequestDto.getClientType() != 0
            ? clientRequestDto.getClientType()
            : null;
    Boolean status = clientRequestDto.getStatus();

    // Filtros por contactos
    String email = clientRequestDto.getEmail();
    String phone = clientRequestDto.getPhone();
    String mobile = clientRequestDto.getMobile();
    String address = clientRequestDto.getAddress();

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Object client: ".concat(String.valueOf(clientRequestDto))),
        REQUEST,
        LEVEL_001);

    log.info(
        "Buscando clientes con: companyId={}, identification={}, name={}, clientType={}, status={}, email={}, phone={}, mobile={}, address={}",
        companyId,
        identification,
        name,
        clientType,
        status,
        email,
        phone,
        mobile,
        address);

    // Campos permitidos para ordenar (solo campos de la entidad Client)
    final List<String> ALLOWED_SORT_FIELDS =
        List.of("idClient", "idType", "identification", "name", "clientType", "status");

    // Mapear el sort para agregar el alias "c." que se usa en la query JPQL
    Pageable mappedPageable = pageable;
    if (pageable.getSort().isSorted()) {
      // Filtrar y validar campos de ordenamiento
      List<Sort.Order> validOrders =
          pageable.getSort().stream()
              .filter(
                  order -> {
                    if (ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                      return true;
                    } else {
                      log.warn("Campo de ordenamiento no válido ignorado: {}", order.getProperty());
                      return false;
                    }
                  })
              .map(
                  order ->
                      new Sort.Order(
                          order.getDirection(),
                          "c." + order.getProperty() // Agregar alias de la query
                          // JPQL
                          ))
              .toList();

      if (!validOrders.isEmpty()) {
        Sort mappedSort = Sort.by(validOrders);
        mappedPageable =
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
        log.info("Sort mapeado: {} -> {}", pageable.getSort(), mappedSort);
      } else {
        // Si todos los campos son inválidos, usar pageable sin sort
        mappedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        log.warn(
            "Todos los campos de ordenamiento son inválidos. Usando paginación sin ordenamiento.");
      }
    }

    Page<ClientDto> clients;
    try {
      clients =
          clientService.findClientByIdentificationOrNameOrType(
              companyId,
              identification,
              name,
              clientType,
              status,
              email,
              phone,
              mobile,
              address,
              mappedPageable);

      log.info("Clientes encontrados: {}", clients.getTotalElements());
    } catch (Exception e) {
      log.error("ERROR al buscar clientes: {}", e.getMessage(), e);
      throw e;
    }

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        RESPONSE,
        LEVEL_001);

    // Devolver la respuesta con el pageable ORIGINAL (sin alias) para que el
    // frontend
    // reciba el sort correcto en los metadatos
    Page<ClientDto> response =
        new PageImpl<>(
            clients.getContent(),
            pageable, // pageable original sin alias
            clients.getTotalElements());

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @PostMapping("search")
  public ResponseEntity<ClientDto> findByIdentificationAndEmail(
      @Valid @RequestBody ClientSearchRequestDto searchRequest) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Search request: " + searchRequest),
        REQUEST,
        LEVEL_001);

    ClientDto client =
        clientService.findByIdentificationAndEmail(
            searchRequest.getIdentification(), searchRequest.getEmail());

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(client),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(client, HttpStatus.OK);
  }

  private static BasicResponse getBasicResponse() {
    return BasicResponse.builder().status(HttpStatus.OK.getReasonPhrase()).build();
  }

  @GetMapping("/identification/{identification}")
  public ResponseEntity<ClientDtoResponse> getClientByIdentification(
      @PathVariable String identification) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Search request: " + identification),
        REQUEST,
        LEVEL_001);

    List<ClientDto> clients = clientService.findByIdentificationList(identification);
    ClientDtoResponse response =
        ClientDtoResponse.builder()
            .status(HttpStatus.OK.getReasonPhrase())
            .clientDtos(clients)
            .build();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(clients),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
