package com.wallparisoft.ebillbilling.aspect;

import com.wallparisoft.ebillbilling.ratelimit.RateLimitType;
import com.wallparisoft.ebillbilling.ratelimit.RateLimited;
import com.wallparisoft.ebillbilling.ratelimit.dto.RateLimitInfo;
import com.wallparisoft.ebillbilling.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspecto AOP para interceptar llamadas a métodos anotados con @RateLimited y aplicar rate
 * limiting.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingAspect {

  private final RateLimitingService rateLimitingService;
  private final com.wallparisoft.ebillbilling.service.ParamsService paramsService;

  @Around("@annotation(rateLimited)")
  public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited)
      throws Throwable {

    // Verificar si rate limiting está habilitado
    boolean enabled =
        Boolean.parseBoolean(paramsService.getParamValueOrDefault("RATE_LIMIT_ENABLED", "true"));
    if (!enabled) {
      return joinPoint.proceed();
    }

    // Obtener request
    HttpServletRequest request = getHttpServletRequest();
    if (request == null) {
      log.warn("No se pudo obtener HttpServletRequest, saltando rate limiting");
      return joinPoint.proceed();
    }

    // Extraer token e IP
    String token = extractToken(request);
    String ipAddress = getClientIpAddress(request);
    RateLimitType rateLimitType = rateLimited.type();
    String endpoint =
        rateLimited.endpoint().isEmpty() ? request.getRequestURI() : rateLimited.endpoint();

    // Log de verificación
    log.debug(
        "Verificando rate limit: endpoint={}, type={}, ip={}, token={}",
        endpoint,
        rateLimitType,
        ipAddress,
        maskToken(token));

    // Verificar límites (lanzará excepción si se excede)
    try {
      rateLimitingService.checkRateLimit(token, ipAddress, rateLimitType);
    } catch (com.wallparisoft.ebillbilling.exception.RateLimitExceededException e) {
      // Log antes de relanzar
      log.warn(
          "🚫 Rate limit excedido en aspecto: endpoint={}, message={}", endpoint, e.getMessage());
      // Relanzar para que GlobalExceptionHandler la capture
      throw e;
    }

    // Obtener información de límites para headers
    RateLimitInfo rateLimitInfo =
        rateLimitingService.getRateLimitInfo(token, ipAddress, rateLimitType);

    // Ejecutar el método
    Object result = joinPoint.proceed();

    // Agregar headers informativos a la respuesta si es ResponseEntity
    if (result instanceof ResponseEntity) {
      ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
      HttpHeaders headers = new HttpHeaders(responseEntity.getHeaders());
      headers.add("X-RateLimit-Limit", String.valueOf(rateLimitInfo.getLimit()));
      headers.add("X-RateLimit-Remaining", String.valueOf(rateLimitInfo.getRemaining()));
      headers.add("X-RateLimit-Reset", String.valueOf(rateLimitInfo.getResetTime()));

      return new ResponseEntity<>(
          responseEntity.getBody(), headers, responseEntity.getStatusCode());
    }

    return result;
  }

  private HttpServletRequest getHttpServletRequest() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    return attributes != null ? attributes.getRequest() : null;
  }

  private String extractToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    return null;
  }

  private String getClientIpAddress(HttpServletRequest request) {
    // Manejar proxies (X-Forwarded-For, X-Real-IP)
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }
    return request.getRemoteAddr();
  }

  private String maskToken(String token) {
    if (token == null || token.length() < 10) {
      return "***";
    }
    // Mostrar solo primeros 4 y últimos 4 caracteres
    return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
  }
}
