package com.wallparisoft.authentication.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.authentication.exception.RateLimitExceededException;
import com.wallparisoft.authentication.service.IUserService;
import com.wallparisoft.authentication.util.RequestUtils;
import com.wallparisoft.ebill.auth.util.exception.InternalErrorException;
import com.wallparisoft.ebill.utils.log.EventLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mail")
@FieldDefaults(level = PRIVATE)
@Slf4j
public class MailController {

  final IUserService userService;

  public MailController(IUserService userService) {

    this.userService = userService;
  }

  @PostMapping(value = "/send", consumes = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<Integer> send(@RequestBody String mail, HttpServletRequest request) {

    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    int result;
    try {
      String clientIp = RequestUtils.extractClientIp(request);
      result = userService.sendMailUser(mail, clientIp);
    } catch (RateLimitExceededException e) {
      // Re-lanzar para que el handler global lo maneje correctamente
      throw e;
    } catch (Exception e) {
      throw new InternalErrorException("Error al enviar correo");
    }
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(result),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
