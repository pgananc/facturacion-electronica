package com.wallparisoft.controller;

import com.wallparisoft.ebill.auth.util.exception.InternalErrorException;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.response.TokenResponse;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallparisoft.dto.UserDto;
import com.wallparisoft.service.ILoginService;

import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/api/login")
@FieldDefaults(level = PRIVATE)
@Log4j2
public class LoginController {


   final ILoginService loginService;



  public LoginController(ILoginService loginService) {
    this.loginService = loginService;

  }


  @PostMapping
  public ResponseEntity<TokenResponse> login(@RequestBody UserDto userDto) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    log.debug(EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .eventType(REQUEST.name())
            .level(LEVEL_001.name())
            .build());
    TokenResponse response  = null;
    try {
      response = loginService.authetication(userDto);
      log.debug(EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(response.getToken())
              .eventType(RESPONSE.name())
              .level(LEVEL_001.name())
              .build());
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (Exception e) {
      throw new InternalErrorException("Error al validar usuario");
    }

  }

}
