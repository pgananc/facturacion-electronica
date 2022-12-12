package com.wallparisoft.ebill.customer.controller;

import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.response.CompanyDtoResponse;
import com.wallparisoft.ebill.customer.service.ICompanyService;
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
import java.util.List;

import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/api/company")
@FieldDefaults(level = PRIVATE)
@Log4j2
public class CompanyController {

    @Autowired
    ICompanyService companyService;

    @PostMapping
    public ResponseEntity<BasicResponse> save(@Valid @RequestBody CompanyDto companyDto) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        companyService.saveCompany(companyDto);
        BasicResponse response = getBasicResponse();
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information(response.getMessage()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BasicResponse> delete(@PathVariable("id") Long id) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information(id).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        companyService.delete(id);
        BasicResponse response = getBasicResponse();
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information(response.getMessage()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/companies-contacts")
    public ResponseEntity<CompanyDtoResponse> findCompaniesActiveAndContactActive() {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        List<CompanyDto> companies = companyService.findCompaniesActiveAndContactActive();
        CompanyDtoResponse response = CompanyDtoResponse.builder().status(HttpStatus.OK.getReasonPhrase()).companyDtos(companies).build();
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information(response.getCompanyDtos()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{idCompany}")
    public ResponseEntity<BasicResponse> update(@Valid @RequestBody CompanyDto companyDto, @PathVariable Long idCompany) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information("ID client: ".concat(idCompany.toString()).concat(", Object company: ").concat(companyDto.toString())).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        companyDto.setIdCompany(idCompany);
        companyService.updateCompany(companyDto);
        BasicResponse response = getBasicResponse();
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information(response.getMessage()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/pageable")
    public ResponseEntity<Page<CompanyDto>> listPageable(Pageable pageable, @Valid @RequestBody CompanyDto companyDto) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information("Object client: ".concat(companyDto.toString())).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        Page<CompanyDto> companies = companyService.findCompanyByIdentificationOrNameOrBranchOfficeCode(companyDto.getIdentification(), companyDto.getName(), companyDto.getBranchOfficeCode(), companyDto.getStatus(), pageable);
        log.info(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(companies, HttpStatus.OK);
    }

    @GetMapping("exist/{identification}/branch/{branchOfficeCode}")
    public ResponseEntity<Boolean> existsByIdentification(@PathVariable(value = "identification") String identification, @PathVariable(value = "branchOfficeCode") String branchOfficeCode) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        Boolean exists = companyService.existsByIdentificationAndBranchOfficeCode(identification, branchOfficeCode);

        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information(exists).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(exists, HttpStatus.OK);
    }

    @GetMapping("/{idCompany}")
    public ResponseEntity<CompanyDtoResponse> getCompanyById(@PathVariable(value = "idCompany") Long idCompany) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        CompanyDto companyDto = companyService.findCompanyById(idCompany);
        CompanyDtoResponse companyDtoResponse = CompanyDtoResponse.builder().companyDto(companyDto).build();
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information(companyDto).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(companyDtoResponse, HttpStatus.OK);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public CompanyDtoResponse getAll() {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        CompanyDtoResponse response = companyService.getAllCompanies();
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information(response.getCompanyDtos()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return response;
    }

    @GetMapping("/ids/{idCompanies}")
    @ResponseStatus(HttpStatus.OK)
    public CompanyDtoResponse getCompaniesByIdCompanies(@PathVariable("idCompanies") Long[] idCompanies) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        CompanyDtoResponse response = companyService.getCompaniesByIdCompanies(List.of(idCompanies));
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information(response.getCompanyDtos()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return response;
    }

    private static BasicResponse getBasicResponse() {
        return BasicResponse.builder().status(HttpStatus.OK.getReasonPhrase()).build();
    }
}