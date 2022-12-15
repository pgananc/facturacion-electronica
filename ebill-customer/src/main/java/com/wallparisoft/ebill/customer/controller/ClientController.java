package com.wallparisoft.ebill.customer.controller;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.response.ClientDtoResponse;
import com.wallparisoft.ebill.customer.service.IClientService;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.websocket.server.PathParam;
import java.util.List;

import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("api/client")
@FieldDefaults(level = PRIVATE)
@Log4j2
public class ClientController {

    @Autowired
    IClientService clientService;


    @PostMapping
    public ResponseEntity<BasicResponse> save(@Valid @RequestBody ClientDto clientDto) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        clientService.saveClientAndContact(clientDto);
        BasicResponse response = getBasicResponse();
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(response.getMessage())
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BasicResponse> delete(@PathVariable("id") Long id) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(id)
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        clientService.delete(id);
        BasicResponse response = getBasicResponse();
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(response.getMessage())
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/company/{companyIdentification}")
    public ResponseEntity<ClientDtoResponse> findByCompanyIdentificationAndClientsActive(@PathVariable("companyIdentification") String companyIdentification) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        List<ClientDto> clients = clientService.findByCompanyIdentificationAndClientsActive(companyIdentification);
        ClientDtoResponse response = ClientDtoResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .clientDtos(clients)
                .build();
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(response.getClientDtos())
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{idClient}")
    public ResponseEntity<ClientDtoResponse> findClientAndContact(@PathVariable(value = "idClient") Long idClient) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        List<ClientDto> clients = clientService.findClientByIdAndContact(idClient);
        ClientDtoResponse response = ClientDtoResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .clientDtos(clients)
                .build();
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(response.getClientDtos())
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @PatchMapping("/{idClient}")
    public ResponseEntity<BasicResponse> update(@Valid @RequestBody ClientDto clientDto, @PathVariable Long idClient) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information("ID client: ".concat(idClient.toString()).concat(", Object company: ").concat(clientDto.toString()))
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        clientService.updateClientAndContact(clientDto, idClient);
        BasicResponse response = getBasicResponse();
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(response.getMessage())
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping ("/pageable")
    public ResponseEntity<Page<ClientDto>> listPageable(Pageable pageable,
                                                          @Valid @RequestBody ClientDto clientDto) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information("Object client: ".concat(clientDto.toString()))
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        Page<ClientDto> clients = clientService.
                findClientByIdentificationOrNameOrType(clientDto.getCompanyIdentification(), clientDto.getIdentification(),
                        clientDto.getName(), clientDto.getClientType(),clientDto.getStatus(),pageable);
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<Page<ClientDto>>(clients, HttpStatus.OK);
    }


    @GetMapping("exist/company/{companyIdentification}/client/{identification}")
    public ResponseEntity<Boolean> existsByIdentification(@PathVariable("companyIdentification") String companyIdentification, @PathVariable(value = "identification") String identification) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        Boolean exists = clientService.existsByCompanyIdentificationAndClientIdentification(companyIdentification, identification);

        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(exists)
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(exists, HttpStatus.OK);
    }
    private static BasicResponse getBasicResponse() {
        BasicResponse response = BasicResponse.builder().status(HttpStatus.OK.getReasonPhrase()).build();
        return response;
    }
}
