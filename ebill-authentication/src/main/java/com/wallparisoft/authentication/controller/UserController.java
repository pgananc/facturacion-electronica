package com.wallparisoft.authentication.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_002;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.authentication.dto.UserDto;
import com.wallparisoft.authentication.dto.UserRequestDto;
import com.wallparisoft.authentication.response.UserDtoResponse;
import com.wallparisoft.authentication.service.IUserService;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/user")
@FieldDefaults(level = PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class UserController {

  final IUserService userService;

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
    userService.delete(id);
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
  public ResponseEntity<Page<UserDto>> listPageable(
      Pageable pageable, @Valid @RequestBody UserDto userDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Object client: ".concat(userDto.toString())),
        REQUEST,
        LEVEL_001);
    Page<UserDto> users =
        userService.findUserByIdCompanyUserNameOrNameOrStatus(
            userDto.getIdCompany(),
            userDto.getName(),
            userDto.getUserName(),
            userDto.getStatus(),
            pageable);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(users, HttpStatus.OK);
  }

  @GetMapping("/company/{idCompany}")
  public ResponseEntity<UserDtoResponse> findUsersActive(
      @PathVariable("idCompany") Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    UserDtoResponse response = userService.findUsersByCompanyAndStatusActive(idCompany);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getUserDtos()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }


  @PostMapping
  public ResponseEntity<BasicResponse> save(@Valid @RequestBody UserRequestDto userRequestDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    userService.registerUserWithRoles(userRequestDto);
    BasicResponse response = getBasicResponse();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getMessage()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @PatchMapping("/{idUser}")
  public ResponseEntity<BasicResponse> update(
      @Valid @RequestBody UserDto userDto, @PathVariable Long idUser) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(
                "ID user: "
                    .concat(idUser.toString())
                    .concat(", Object company: ")
                    .concat(userDto.toString())),
        REQUEST,
        LEVEL_001);
    userService.update(userDto, idUser);
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

  @GetMapping("exist/userName/{userName}")
  public ResponseEntity<Boolean> existsByUserUserName(
      @PathVariable(value = "userName") String userName) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    Boolean exists = userService.existsByUserName(userName);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(exists),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(exists, HttpStatus.OK);
  }

  @PostMapping(value = "/restore/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Boolean> restorePassword(
      @PathVariable("token") String token, @RequestBody String password) {
    Boolean result;
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    result = userService.resetPassword(token, password);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(result),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @GetMapping("/not-company/{idCompany}")
  public ResponseEntity<UserDtoResponse> findActiveUsersAndNotInCompany(
      @PathVariable("idCompany") Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    UserDtoResponse response = userService.findActiveUsersAndNotInCompany(idCompany);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getUserDtos()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("exist/mail/{mail}")
  public ResponseEntity<Boolean> existsByMail(@PathVariable(value = "mail") String mail) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    Boolean exists = userService.existsByMail(mail);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(exists),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(exists, HttpStatus.OK);
  }

  @PostMapping("userName/password")
  public ResponseEntity<UserDtoResponse> findUserByUserNameAndPassword(
      @RequestBody UserDto userDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Validating credentials for userName: " + userDto.getUserName()),
        REQUEST,
        LEVEL_001);
    
    try {
      UserDtoResponse userDtoResponse =
          userService.findUserByUserNameAndPassword(userDto.getUserName(), userDto.getPassword());
      
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(
                  String.format(
                      "Credentials validated successfully for user: %s (ID: %d)",
                      userDto.getUserName(),
                      userDtoResponse.getUserDto() != null ? userDtoResponse.getUserDto().getIdUser() : null)),
          RESPONSE,
          LEVEL_001);
      return new ResponseEntity<>(userDtoResponse, HttpStatus.OK);
    } catch (Exception e) {
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Validation failed: " + e.getMessage()),
          RESPONSE,
          LEVEL_002);
      throw e;
    }
  }

  @PatchMapping("{idUser}/password")
  public ResponseEntity<Boolean> changePassword(
      @RequestBody UserDto userDto, @PathVariable Long idUser) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    try {
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Changing password for user ID: " + idUser),
          REQUEST,
          LEVEL_001);
      
      if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
        throw new IllegalArgumentException("Password cannot be null or empty");
      }
      
      userService.changePassword(userDto.getPassword(), idUser);
      
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Password changed successfully for user ID: " + idUser),
          RESPONSE,
          LEVEL_001);
      return new ResponseEntity<>(true, HttpStatus.OK);
    } catch (Exception e) {
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Error changing password: " + e.getMessage()),
          RESPONSE,
          LEVEL_002);
      throw new RuntimeException(e);
    }
  }

  @GetMapping("userName/{userName}")
  public ResponseEntity<UserDtoResponse> findUserAndRol(
      @PathVariable(value = "userName") String userName) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    UserDtoResponse response = userService.findUserByUserName(userName);
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
