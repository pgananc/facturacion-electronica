package com.wallparisoft.authentication.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.authentication.response.RoleDtoResponse;
import com.wallparisoft.authentication.service.IRoleService;
import com.wallparisoft.ebill.utils.log.EventLog;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/role")
@FieldDefaults(level = PRIVATE)
@Slf4j
public class RoleController {

  private final IRoleService roleService;

  public RoleController(IRoleService roleService) {
    this.roleService = roleService;
  }

  @GetMapping
  public ResponseEntity<RoleDtoResponse> findAll() {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);

    RoleDtoResponse response = roleService.findAllActive();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getRoleDtos()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/administration")
  public ResponseEntity<RoleDtoResponse> findAllActiveWithoutSuperAdmin() {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    RoleDtoResponse response = roleService.findAllActiveWithoutSuperAdmin();
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getRoleDtos()),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
