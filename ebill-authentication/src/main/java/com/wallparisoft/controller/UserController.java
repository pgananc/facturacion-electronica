package com.wallparisoft.controller;

import com.wallparisoft.dto.UserDto;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.response.BasicResponse;
import com.wallparisoft.response.UserDtoResponse;
import com.wallparisoft.service.ITokenService;
import com.wallparisoft.service.IUserService;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("api/user")
@FieldDefaults(level = PRIVATE)
@Log4j2
public class UserController {

    private final IUserService userService;

    private final ITokenService tokenService;

    public UserController(IUserService userService, ITokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
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
        userService.delete(id);
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

    @PostMapping("/pageable")
    public ResponseEntity<Page<UserDto>> listPageable(Pageable pageable,
                                                      @Valid @RequestBody UserDto userDto) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information("Object client: ".concat(userDto.toString()))
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        Page<UserDto> users = userService.
                findUserByUserNameOrNameOrStatus(userDto.getName(),
                        userDto.getUserName(), userDto.getStatus(), pageable);
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<Page<UserDto>>(users, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<UserDtoResponse> findUsersActive() {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        UserDtoResponse response = userService.findUsersActive();

        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(response.getUserDtos())
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{idUser}")
    public ResponseEntity<UserDtoResponse> findUserAndRol(@PathVariable(value = "idUser") Long idUser) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        UserDtoResponse response = userService.findUserWithRolById(idUser);

        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(response.getUserDtos())
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<BasicResponse> save(@Valid @RequestBody UserDto userDto) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        userService.saveUserAndRole(userDto);
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

    @PatchMapping("/{idUser}")
    public ResponseEntity<BasicResponse> update(@Valid @RequestBody UserDto userDto, @PathVariable Long idUser) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information("ID user: ".concat(idUser.toString()).concat(", Object company: ").concat(userDto.toString()))
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        userService.update(userDto, idUser);
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

    @GetMapping("exist/{userName}")
    public ResponseEntity<Boolean> existsByUserName(@PathVariable(value = "userName") String userName) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        Boolean exists = userService.existsByUserName(userName);

        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(exists)
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(exists, HttpStatus.OK);
    }


    @PostMapping(value = "/restore/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> restorePassword(@PathVariable("token") String token, @RequestBody String password) {
        Boolean result = Boolean.FALSE;
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        result = userService.resetPassword(token, password);
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(result)
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/company/{idCompany}")
    public ResponseEntity<UserDtoResponse> findActiveUsersAndNotInCompany(@PathVariable("idCompany") Long idCompany) {
        StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .eventType(REQUEST.name())
                .level(LEVEL_001.name())
                .build());
        UserDtoResponse response = userService.findActiveUsersAndNotInCompany(idCompany);

        log.debug(EventLog.builder()
                .service(traceElement.getClassName())
                .method(traceElement.getMethodName())
                .information(response.getUserDtos())
                .eventType(RESPONSE.name())
                .level(LEVEL_001.name())
                .build());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private static BasicResponse getBasicResponse() {
        BasicResponse response = BasicResponse.builder().status(HttpStatus.OK.getReasonPhrase()).build();
        return response;
    }
}
