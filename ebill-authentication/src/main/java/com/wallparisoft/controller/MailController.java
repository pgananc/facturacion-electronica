package com.wallparisoft.controller;

import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.entity.User;
import com.wallparisoft.response.RoleDtoResponse;
import com.wallparisoft.service.IUserService;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/api/mail")
@FieldDefaults(level = PRIVATE)
@Log4j2
public class MailController {



     final IUserService userService;

    public MailController(IUserService userService) {

        this.userService = userService;
    }

    @PostMapping(value = "/send", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Integer> send(@RequestBody String mail) {

        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        int result = userService.sendMailUser(mail);
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(result)
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
