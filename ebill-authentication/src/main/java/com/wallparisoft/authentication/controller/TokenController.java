package com.wallparisoft.authentication.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.authentication.dto.ClientTokenRequestDto;
import com.wallparisoft.authentication.dto.ClientTokenValidationRequest;
import com.wallparisoft.authentication.entity.ClientToken;
import com.wallparisoft.authentication.response.TokenResponse;
import com.wallparisoft.authentication.service.IClientTokenService;
import com.wallparisoft.authentication.service.ITokenService;
import com.wallparisoft.authentication.util.RequestUtils;
import com.wallparisoft.ebill.utils.log.EventLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/token")
@FieldDefaults(level = PRIVATE)
@Slf4j
public class TokenController {

  final ITokenService tokenService;
  final IClientTokenService clientTokenService;

  public TokenController(ITokenService tokenService, IClientTokenService clientTokenService) {
    this.tokenService = tokenService;
    this.clientTokenService = clientTokenService;
  }

  @GetMapping(value = "/restore-password/validate/{token}")
  public ResponseEntity<Boolean> resetPassword(@PathVariable("token") String token) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    Boolean result = tokenService.validateActiveToken(token);
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(result),
        RESPONSE,
        LEVEL_001);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  @PostMapping("/client")
  public ResponseEntity<ClientToken> saveClientToken(
      @RequestBody ClientTokenRequestDto request, HttpServletRequest httpRequest) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);

    String clientIp = RequestUtils.extractClientIp(httpRequest);
    ClientToken result =
        clientTokenService.saveClientToken(
            request.getClientNumber(), request.getClientEmail(), clientIp);

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(result),
        RESPONSE,
        LEVEL_001);

    return new ResponseEntity<>(result, HttpStatus.CREATED);
  }

  @PostMapping("/resend-client")
  public ResponseEntity<ClientToken> resendClientToken(
      @RequestBody ClientTokenRequestDto request, HttpServletRequest httpRequest) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);

    String clientIp = RequestUtils.extractClientIp(httpRequest);
    ClientToken result =
        clientTokenService.resendClientToken(
            request.getClientNumber(), request.getClientEmail(), clientIp);

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(result),
        RESPONSE,
        LEVEL_001);

    return new ResponseEntity<>(result, HttpStatus.CREATED);
  }

  @PostMapping("/validate-client/generate")
  public ResponseEntity<TokenResponse> validateTokenAndGenerateAccessToken(
      @RequestBody ClientTokenValidationRequest request, HttpServletRequest httpRequest) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);

    String clientIp = RequestUtils.extractClientIp(httpRequest);
    TokenResponse result =
        clientTokenService.validateTokenAndGenerateAccessToken(
            request.getClientNumber(), request.getToken(), clientIp);

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
