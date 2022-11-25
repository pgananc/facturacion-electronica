package com.wallparisoft.controller;

import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.entity.ResetToken;
import com.wallparisoft.service.IRestTokenService;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/api/token")
@FieldDefaults(level = PRIVATE)
@Log4j2
public class TokenController {


   final IRestTokenService restTokenService;

  public TokenController(IRestTokenService restTokenService) {
    this.restTokenService = restTokenService;
  }

  @GetMapping(value = "/restore-password/validate/{token}")
  public ResponseEntity<Boolean> restablecerClave(@PathVariable("token") String token) {
    Boolean result = false;
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    log.debug(EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .eventType(REQUEST.name())
            .level(LEVEL_001.name())
            .build());

      if (token != null && !token.isEmpty()) {
        result = restTokenService.validateTokenActive(token);
      }
      log.debug(EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(result)
              .eventType(RESPONSE.name())
              .level(LEVEL_001.name())
              .build());
    return new ResponseEntity<Boolean>(result, HttpStatus.OK);
  }
}
