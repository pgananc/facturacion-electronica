package com.wallparisoft.authentication.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.authentication.dto.UserCompanyDto;
import com.wallparisoft.authentication.dto.UserCompanyRequestDto;
import com.wallparisoft.authentication.dto.UserDto;
import com.wallparisoft.authentication.entity.UserCompany;
import com.wallparisoft.authentication.response.UserCompanyResponse;
import com.wallparisoft.authentication.response.UserDtoResponse;
import com.wallparisoft.authentication.service.IUserCompanyService;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-company")
@FieldDefaults(level = PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class UserCompanyController {

  final IUserCompanyService userCompanyService;

  @PostMapping
  public ResponseEntity<BasicResponse> save(
      @Valid @RequestBody UserCompanyRequestDto userCompanyRequestDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    UserCompany userCompany =
        userCompanyService.saveUserCompany(
            userCompanyRequestDto.getIdCompany(), userCompanyRequestDto.getIdUser());
    BasicResponse response = getBasicResponse();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getMessage()),
        RESPONSE,
        LEVEL_001);
    if (userCompany == null) {
      response.setCode(HttpStatus.CONFLICT.value());
      response.setMessage("User id already exists for that company");
      response.setStatus("ERROR");
      return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @DeleteMapping("/company/{idCompany}/user/{idUser}")
  public ResponseEntity<BasicResponse> delete(
      @PathVariable("idCompany") Long idCompany, @PathVariable("idUser") Long idUser) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("id company: " + idCompany + ", id user: " + idUser),
        REQUEST,
        LEVEL_001);
    userCompanyService.delete(idCompany, idUser);
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

  @GetMapping("/company/{idCompany}/user/{idUser}")
  public ResponseEntity<UserCompanyResponse> getByIdCompanyAndIdUser(
      @PathVariable(value = "idCompany") Long idCompany,
      @PathVariable(value = "idUser") Long idUser) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    UserCompanyDto userCompanyDto = userCompanyService.findByIdCompanyAndIdUser(idCompany, idUser);
    UserCompanyResponse userCompanyResponse =
        UserCompanyResponse.builder().userCompanyDto(userCompanyDto).build();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(userCompanyDto),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(userCompanyResponse, HttpStatus.OK);
  }

  @GetMapping("/company/{idCompany}")
  public ResponseEntity<UserCompanyResponse> getUsersByIdCompany(
      @PathVariable(value = "idCompany") Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    UserCompanyDto userCompanyDto = userCompanyService.getUsersFromACompany(idCompany);
    UserCompanyResponse userCompanyResponse =
        UserCompanyResponse.builder().userCompanyDto(userCompanyDto).build();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(userCompanyDto),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(userCompanyResponse, HttpStatus.OK);
  }

  @PostMapping("/pageable")
  public ResponseEntity<Page<UserDto>> listPageable(
      Pageable pageable, @Valid @RequestBody Long companyId) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    Page<UserDto> usersFromACompanyPageable =
        userCompanyService.getUsersFromACompanyPageable(companyId, pageable);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(usersFromACompanyPageable, HttpStatus.OK);
  }

  @GetMapping("/user/{idUser}/company/{idCompany}")
  public ResponseEntity<UserDtoResponse> findUserAndRolesByCompany(
      @PathVariable(value = "idUser") Long idUser,
      @PathVariable(value = "idCompany") Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("idUser: " + idUser + ", idCompany: " + idCompany),
        REQUEST,
        LEVEL_001);
    UserDtoResponse response =
        userCompanyService.findUserWithRolesByUserIdAndCompanyId(idUser, idCompany);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getUserDtos()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  private static BasicResponse getBasicResponse() {
    return BasicResponse.builder().status(HttpStatus.OK.getReasonPhrase()).build();
  }
}
