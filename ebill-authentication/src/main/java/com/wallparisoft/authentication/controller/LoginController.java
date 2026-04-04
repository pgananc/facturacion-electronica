package com.wallparisoft.authentication.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.authentication.dto.UserLoginRequestDto;
import com.wallparisoft.authentication.response.TokenResponse;
import com.wallparisoft.authentication.service.ILoginService;
import com.wallparisoft.ebill.auth.util.exception.InternalErrorException;
import com.wallparisoft.ebill.utils.log.EventLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/login")
@FieldDefaults(level = PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class LoginController {
  final ILoginService loginService;

  @PostMapping
  public ResponseEntity<TokenResponse> login(
      @RequestBody UserLoginRequestDto userLoginRequestDto, HttpServletRequest request) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    TokenResponse response;
    try {
      // Extraer IP y User-Agent para fingerprint de sesión
      String clientIp = extractClientIp(request);
      String userAgent = request.getHeader("User-Agent");

      response = loginService.authentication(userLoginRequestDto, clientIp, userAgent);
      setLogs(
          EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(response.getToken()),
          RESPONSE,
          LEVEL_001);
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new InternalErrorException("Error al validar usuario");
    }
  }

  /**
   * Extrae la IP real del cliente considerando proxies y load balancers.
   *
   * @param request HttpServletRequest
   * @return IP del cliente
   */
  private String extractClientIp(HttpServletRequest request) {
    // X-Forwarded-For puede contener múltiples IPs si hay varios proxies
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      String[] ips = xForwardedFor.split(",");
      return ips[0].trim();
    }

    // X-Real-IP es usado por Nginx
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp.trim();
    }

    return request.getRemoteAddr();
  }

  @DeleteMapping("/logout")
  public ResponseEntity<Void> logout(@RequestParam String userName) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Logout para usuario: " + userName),
        REQUEST,
        LEVEL_001);

    loginService.logoutUser(userName);

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Sesión cerrada y punto de emisión liberado"),
        RESPONSE,
        LEVEL_001);

    return ResponseEntity.noContent().build();
  }
}
