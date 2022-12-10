package com.wallparisoft.controller;

import com.wallparisoft.dto.UserCompanyDto;
import com.wallparisoft.dto.UserCompanyRequestDto;
import com.wallparisoft.dto.UserDto;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import com.wallparisoft.entity.UserCompany;
import com.wallparisoft.response.UserCompanyResponse;
import com.wallparisoft.service.IUserCompanyService;
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
@RequestMapping("/api/user-company")
@FieldDefaults(level = PRIVATE)
@Log4j2
public class UserCompanyController {

    @Autowired
    IUserCompanyService userCompanyService;

    @PostMapping
    public ResponseEntity<BasicResponse> save(@Valid @RequestBody UserCompanyRequestDto userCompanyRequestDto) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName())
                .method(traceElement.getMethodName()).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        UserCompany userCompany = userCompanyService.saveUserCompany(userCompanyRequestDto.getIdCompany(), userCompanyRequestDto.getIdUser());
        BasicResponse response = getBasicResponse();
        log.debug(EventLog.builder().service(traceElement.getClassName())
                .method(traceElement.getMethodName()).information(response.getMessage()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        if (userCompany == null) {
            response.setCode(HttpStatus.CONFLICT.value());
            response.setMessage("User id already exists for that company");
            response.setStatus("ERROR");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/company/{idCompany}/user/{idUser}")
    public ResponseEntity<BasicResponse> delete(@PathVariable("idCompany") Long idCompany, @PathVariable("idUser") Long idUser) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName())
                .method(traceElement.getMethodName()).information("id company: " + idCompany + ", id user: " + idUser)
                .eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        userCompanyService.delete(idCompany, idUser);
        BasicResponse response = getBasicResponse();
        log.debug(EventLog.builder().service(traceElement.getClassName())
                .method(traceElement.getMethodName()).information(response.getMessage()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/company/{idCompany}/user/{idUser}")
    public ResponseEntity<UserCompanyResponse> getByIdCompanyAndIdUser(@PathVariable(value = "idCompany") Long idCompany,
                                                                         @PathVariable(value = "idUser") Long idUser) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName())
                .method(traceElement.getMethodName()).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        UserCompanyDto userCompanyDto = userCompanyService.findByIdCompanyAndIdUser(idCompany, idUser);
        UserCompanyResponse userCompanyResponse = UserCompanyResponse.builder().userCompanyDto(userCompanyDto).build();
        log.debug(EventLog.builder().service(traceElement.getClassName())
                .method(traceElement.getMethodName()).information(userCompanyDto).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(userCompanyResponse, HttpStatus.OK);
    }

    @GetMapping("/company/{idCompany}")
    public ResponseEntity<UserCompanyResponse> getUsersByIdCompany(@PathVariable(value = "idCompany") Long idCompany) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        UserCompanyDto userCompanyDto = userCompanyService.getUsersFromACompany(idCompany);
        UserCompanyResponse userCompanyResponse = UserCompanyResponse.builder().userCompanyDto(userCompanyDto).build();
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName())
                .information(userCompanyDto).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(userCompanyResponse, HttpStatus.OK);
    }

    @PostMapping("/pageable")
    public ResponseEntity<Page<UserDto>> listPageable(Pageable pageable, @Valid @RequestBody Long companyId) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).information("Object user: ".concat(companyId.toString())).eventType(REQUEST.name()).level(LEVEL_001.name()).build());
        Page<UserDto> usersFromACompanyPageable = userCompanyService.getUsersFromACompanyPageable(companyId, pageable);
        log.info(EventLog.builder().service(traceElement.getClassName()).method(traceElement.getMethodName()).eventType(RESPONSE.name()).level(LEVEL_001.name()).build());
        return new ResponseEntity<>(usersFromACompanyPageable, HttpStatus.OK);
    }

    private static BasicResponse getBasicResponse() {
        return BasicResponse.builder().status(HttpStatus.OK.getReasonPhrase()).build();
    }
}