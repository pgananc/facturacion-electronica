package com.wallparisoft.ebill.customer.controller;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.CompanyClientDto;
import com.wallparisoft.ebill.customer.dto.CompanyClientRequestDto;
import com.wallparisoft.ebill.customer.entity.CompanyClient;
import com.wallparisoft.ebill.customer.response.CompanyClientResponse;
import com.wallparisoft.ebill.customer.service.ICompanyClientService;
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

import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/api/company-client")
@FieldDefaults(level = PRIVATE)
@Log4j2
public class CompanyClientController {

    @Autowired
    ICompanyClientService companyClientService;

    @PostMapping
    public ResponseEntity<BasicResponse> save(@Valid @RequestBody CompanyClientRequestDto companyClientRequestDto) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName())
                .method(traceElement.getMethodName()).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        CompanyClient companyClient = companyClientService.saveCompanyClient(companyClientRequestDto.getCompanyIdentification(), companyClientRequestDto.getIdClient());
        BasicResponse response = getBasicResponse();
        log.debug(EventLog.builder().service(traceElement.getClassName())
                .method(traceElement.getMethodName()).information(response.getMessage()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        if (companyClient == null) {
            response.setCode(HttpStatus.CONFLICT.value());
            response.setMessage("Client id already exists for that company");
            response.setStatus("ERROR");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/company/{companyIdentification}/client/{idClient}")
    public ResponseEntity<BasicResponse> delete(@PathVariable("companyIdentification") String companyIdentification, @PathVariable("idClient") Long idClient) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName())
                .method(traceElement.getMethodName()).information("id company: " + companyIdentification + ", id client: " + idClient)
                .eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        companyClientService.delete(companyIdentification, idClient);
        BasicResponse response = getBasicResponse();
        log.debug(EventLog.builder().service(traceElement.getClassName())
                .method(traceElement.getMethodName()).information(response.getMessage()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/company/{companyIdentification}/client/{idClient}")
    public ResponseEntity<CompanyClientResponse> getByIdCompanyAndIdClient(@PathVariable(value = "companyIdentification") String companyIdentification,
                                                                           @PathVariable(value = "idClient") Long idClient) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName())
                .method(traceElement.getMethodName()).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        CompanyClientDto companyClientDto = companyClientService.findByCompanyIdentificationAndIdClient(companyIdentification, idClient);
        CompanyClientResponse companyClientResponse = CompanyClientResponse.builder().companyClientDto(companyClientDto).build();
        log.debug(EventLog.builder().service(traceElement.getClassName())
                .method(traceElement.getMethodName()).information(companyClientDto).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(companyClientResponse, HttpStatus.OK);
    }

    @GetMapping("/company/{companyIdentification}")
    public ResponseEntity<CompanyClientResponse> getClientsByIdCompany(@PathVariable(value = "companyIdentification") String companyIdentification) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        CompanyClientDto companyClientDto = companyClientService.getClientsFromACompany(companyIdentification);
        CompanyClientResponse companyClientResponse = CompanyClientResponse.builder().companyClientDto(companyClientDto).build();
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName())
                .information(companyClientDto).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(companyClientResponse, HttpStatus.OK);
    }

    @PostMapping("/pageable")
    public ResponseEntity<Page<ClientDto>> listPageable(Pageable pageable, @Valid @RequestBody String companyIdentification) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information("Object client: ".concat(companyIdentification)).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        Page<ClientDto> clientsFromACompanyPageable = companyClientService.getClientsFromACompanyPageable(companyIdentification, pageable);
        log.info(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(clientsFromACompanyPageable, HttpStatus.OK);
    }

    private static BasicResponse getBasicResponse() {
        return BasicResponse.builder().status(HttpStatus.OK.getReasonPhrase()).build();
    }
}