package com.wallparisoft.authentication.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.EventType.RESPONSE;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;

import com.wallparisoft.authentication.dto.SessionValidationResponseDto;
import com.wallparisoft.authentication.service.ISessionFingerprintService;
import com.wallparisoft.ebill.utils.log.EventLog;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

  private final ISessionFingerprintService sessionFingerprintService;

  /**
   * Valida que la sesión actual coincide con el fingerprint guardado al momento del login.
   *
   * <p>Verifica: - IP del cliente coincide con la IP del login - Navegador coincide con el del
   * login - Token no expirado (validado por Spring Security antes de llegar aquí) - Estado del
   * punto de emisión asignado
   *
   * @param principal Usuario autenticado (extraído del JWT por Spring Security)
   * @param request HttpServletRequest para obtener IP y User-Agent
   * @return SessionValidationResponseDto con el resultado de la validación
   */
  @GetMapping("/validate")
  public ResponseEntity<SessionValidationResponseDto> validateSession(
      Principal principal, HttpServletRequest request) {

    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);

    String userName = principal.getName();
    String clientIp = extractClientIp(request);
    String userAgent = request.getHeader("User-Agent");

    log.debug("🔍 Validando sesión para usuario: {}, IP: {}", userName, clientIp);

    SessionValidationResponseDto response =
        sessionFingerprintService.validateSession(userName, clientIp, userAgent);

    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Resultado validación: " + response.isValid()),
        RESPONSE,
        LEVEL_001);

    return ResponseEntity.ok(response);
  }

  /**
   * Extrae la IP real del cliente considerando proxies y load balancers.
   *
   * @param request HttpServletRequest
   * @return IP del cliente
   */
  private String extractClientIp(HttpServletRequest request) {
    // X-Forwarded-For puede contener múltiples IPs si hay varios proxies
    // El formato es: client, proxy1, proxy2, ...
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      // Tomamos la primera IP (el cliente original)
      String[] ips = xForwardedFor.split(",");
      return ips[0].trim();
    }

    // X-Real-IP es usado por Nginx
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp.trim();
    }

    // Fallback a la IP remota directa
    return request.getRemoteAddr();
  }
}
