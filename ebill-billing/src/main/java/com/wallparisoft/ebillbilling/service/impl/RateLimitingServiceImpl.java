package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.exception.RateLimitExceededException;
import com.wallparisoft.ebillbilling.ratelimit.RateLimitType;
import com.wallparisoft.ebillbilling.ratelimit.dto.RateLimitInfo;
import com.wallparisoft.ebillbilling.service.ParamsService;
import com.wallparisoft.ebillbilling.service.RateLimitingService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

/** Implementación del servicio de rate limiting usando Redis con algoritmo Sliding Window Log. */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingServiceImpl implements RateLimitingService {

  private static final String RATE_LIMIT_PREFIX = "rate_limit:";
  private static final String PARAM_QUERY_REQ_PER_MIN_TOKEN = "RATE_LIMIT_QUERY_REQ_PER_MIN_TOKEN";
  private static final String PARAM_QUERY_REQ_PER_HOUR_IP = "RATE_LIMIT_QUERY_REQ_PER_HOUR_IP";
  private static final String PARAM_DOWNLOAD_PER_HOUR_TOKEN = "RATE_LIMIT_DOWNLOAD_PER_HOUR_TOKEN";
  private static final String PARAM_DOWNLOAD_PER_HOUR_IP = "RATE_LIMIT_DOWNLOAD_PER_HOUR_IP";
  private static final String PARAM_ENABLED = "RATE_LIMIT_ENABLED";

  private final StringRedisTemplate redisTemplate;
  private final ParamsService paramsService;
  private boolean redisAvailable = true;

  @Override
  public void checkRateLimit(String token, String ipAddress, RateLimitType rateLimitType) {
    // Verificar si rate limiting está habilitado
    boolean enabled =
        Boolean.parseBoolean(paramsService.getParamValueOrDefault(PARAM_ENABLED, "true"));
    if (!enabled) {
      return;
    }

    try {
      if (rateLimitType == RateLimitType.QUERY) {
        checkQueryLimits(token, ipAddress);
      } else if (rateLimitType == RateLimitType.DOWNLOAD) {
        checkDownloadLimits(token, ipAddress);
      }
      redisAvailable = true;
    } catch (RateLimitExceededException e) {
      // Relanzar excepción de límite excedido - es el comportamiento esperado
      redisAvailable = true; // Redis está funcionando, solo se excedió el límite
      log.info("🚫 Lanzando RateLimitExceededException: {}", e.getMessage());
      throw e;
    } catch (DataAccessException e) {
      // Error real de conexión/acceso a Redis
      redisAvailable = false;
      log.warn("Redis no disponible para rate limiting, permitiendo request: {}", e.getMessage());
      // Graceful degradation: permitir request si Redis falla
      // En producción, considerar fallback más estricto según política
    } catch (Exception e) {
      // Otros errores inesperados
      redisAvailable = false;
      log.error("Error inesperado en rate limiting, permitiendo request: {}", e.getMessage(), e);
      // Graceful degradation: permitir request si hay error inesperado
    }
  }

  @Override
  public RateLimitInfo getRateLimitInfo(
      String token, String ipAddress, RateLimitType rateLimitType) {
    try {
      if (rateLimitType == RateLimitType.QUERY) {
        return getQueryLimitInfo(token, ipAddress);
      } else {
        return getDownloadLimitInfo(token, ipAddress);
      }
    } catch (Exception e) {
      log.debug("Error al obtener información de rate limit: {}", e.getMessage());
      // Retornar información por defecto si hay error
      return RateLimitInfo.builder()
          .limit(100)
          .remaining(100)
          .resetTime(Instant.now().plusSeconds(60).getEpochSecond())
          .limitType("UNKNOWN")
          .timeWindow("UNKNOWN")
          .build();
    }
  }

  private void checkQueryLimits(String token, String ipAddress) {
    // Límite por token (por minuto)
    if (token != null && !token.isBlank()) {
      int limitPerMinToken =
          Integer.parseInt(
              paramsService.getParamValueOrDefault(PARAM_QUERY_REQ_PER_MIN_TOKEN, "100"));
      String tokenKey = buildKey("query", "token", token, "minute");
      if (!checkLimit(tokenKey, limitPerMinToken, 60)) {
        RateLimitInfo info = getLimitInfo(tokenKey, limitPerMinToken, 60, "TOKEN", "MINUTE");
        throw new RateLimitExceededException(
            String.format(
                "Límite de consultas excedido: %d solicitudes por minuto por token. Intente nuevamente más tarde.",
                limitPerMinToken),
            info);
      }
    }

    // Límite por IP (por hora)
    if (ipAddress != null && !ipAddress.isBlank()) {
      int limitPerHourIp =
          Integer.parseInt(
              paramsService.getParamValueOrDefault(PARAM_QUERY_REQ_PER_HOUR_IP, "500"));
      String ipKey = buildKey("query", "ip", ipAddress, "hour");
      if (!checkLimit(ipKey, limitPerHourIp, 3600)) {
        RateLimitInfo info = getLimitInfo(ipKey, limitPerHourIp, 3600, "IP", "HOUR");
        throw new RateLimitExceededException(
            String.format(
                "Límite de consultas excedido: %d solicitudes por hora por IP. Intente nuevamente más tarde.",
                limitPerHourIp),
            info);
      }
    }
  }

  private void checkDownloadLimits(String token, String ipAddress) {
    // Límite por token (por hora)
    if (token != null && !token.isBlank()) {
      int limitPerHourToken =
          Integer.parseInt(
              paramsService.getParamValueOrDefault(PARAM_DOWNLOAD_PER_HOUR_TOKEN, "50"));
      String tokenKey = buildKey("download", "token", token, "hour");
      if (!checkLimit(tokenKey, limitPerHourToken, 3600)) {
        RateLimitInfo info = getLimitInfo(tokenKey, limitPerHourToken, 3600, "TOKEN", "HOUR");
        throw new RateLimitExceededException(
            String.format(
                "Límite de descargas excedido: %d descargas por hora por token. Intente nuevamente más tarde.",
                limitPerHourToken),
            info);
      }
    }

    // Límite por IP (por hora)
    if (ipAddress != null && !ipAddress.isBlank()) {
      int limitPerHourIp =
          Integer.parseInt(paramsService.getParamValueOrDefault(PARAM_DOWNLOAD_PER_HOUR_IP, "200"));
      String ipKey = buildKey("download", "ip", ipAddress, "hour");
      if (!checkLimit(ipKey, limitPerHourIp, 3600)) {
        RateLimitInfo info = getLimitInfo(ipKey, limitPerHourIp, 3600, "IP", "HOUR");
        throw new RateLimitExceededException(
            String.format(
                "Límite de descargas excedido: %d descargas por hora por IP. Intente nuevamente más tarde.",
                limitPerHourIp),
            info);
      }
    }
  }

  private RateLimitInfo getQueryLimitInfo(String token, String ipAddress) {
    // Retornar información del límite más restrictivo
    if (token != null && !token.isBlank()) {
      int limit =
          Integer.parseInt(
              paramsService.getParamValueOrDefault(PARAM_QUERY_REQ_PER_MIN_TOKEN, "100"));
      String tokenKey = buildKey("query", "token", token, "minute");
      return getLimitInfo(tokenKey, limit, 60, "TOKEN", "MINUTE");
    } else if (ipAddress != null && !ipAddress.isBlank()) {
      int limit =
          Integer.parseInt(
              paramsService.getParamValueOrDefault(PARAM_QUERY_REQ_PER_HOUR_IP, "500"));
      String ipKey = buildKey("query", "ip", ipAddress, "hour");
      return getLimitInfo(ipKey, limit, 3600, "IP", "HOUR");
    }
    return RateLimitInfo.builder().limit(100).remaining(100).build();
  }

  private RateLimitInfo getDownloadLimitInfo(String token, String ipAddress) {
    if (token != null && !token.isBlank()) {
      int limit =
          Integer.parseInt(
              paramsService.getParamValueOrDefault(PARAM_DOWNLOAD_PER_HOUR_TOKEN, "50"));
      String tokenKey = buildKey("download", "token", token, "hour");
      return getLimitInfo(tokenKey, limit, 3600, "TOKEN", "HOUR");
    } else if (ipAddress != null && !ipAddress.isBlank()) {
      int limit =
          Integer.parseInt(paramsService.getParamValueOrDefault(PARAM_DOWNLOAD_PER_HOUR_IP, "200"));
      String ipKey = buildKey("download", "ip", ipAddress, "hour");
      return getLimitInfo(ipKey, limit, 3600, "IP", "HOUR");
    }
    return RateLimitInfo.builder().limit(50).remaining(50).build();
  }

  /**
   * Verifica si el límite se ha excedido usando Sliding Window Log.
   *
   * @param key Clave Redis
   * @param limit Límite máximo
   * @param windowSeconds Ventana de tiempo en segundos
   * @return true si el límite no se ha excedido, false en caso contrario
   */
  private boolean checkLimit(String key, int limit, int windowSeconds) {
    long now = System.currentTimeMillis();
    long windowStart = now - (windowSeconds * 1000L);

    ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

    // Agregar timestamp actual
    zSetOps.add(key, String.valueOf(now), now);

    // Remover timestamps fuera de la ventana
    zSetOps.removeRangeByScore(key, 0, windowStart);

    // Contar requests en la ventana
    Long count = zSetOps.count(key, windowStart, now);

    // Establecer TTL (un poco más que la ventana para evitar race conditions)
    redisTemplate.expire(key, Duration.ofSeconds(windowSeconds + 10));

    return count != null && count <= limit;
  }

  /**
   * Obtiene información sobre el límite actual.
   *
   * @param key Clave Redis
   * @param limit Límite máximo
   * @param windowSeconds Ventana de tiempo en segundos
   * @param limitType Tipo de límite (TOKEN o IP)
   * @param timeWindow Ventana de tiempo (MINUTE o HOUR)
   * @return Información del límite
   */
  private RateLimitInfo getLimitInfo(
      String key, int limit, int windowSeconds, String limitType, String timeWindow) {
    long now = System.currentTimeMillis();
    long windowStart = now - (windowSeconds * 1000L);

    ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
    Long count = zSetOps.count(key, windowStart, now);

    int currentCount = (count != null) ? count.intValue() : 0;
    int remaining = Math.max(0, limit - currentCount);
    long resetTime = Instant.now().plusSeconds(windowSeconds).getEpochSecond();

    return RateLimitInfo.builder()
        .limit(limit)
        .remaining(remaining)
        .resetTime(resetTime)
        .limitType(limitType)
        .timeWindow(timeWindow)
        .build();
  }

  private String buildKey(String type, String identifierType, String identifier, String window) {
    // Sanitizar identifier para evitar caracteres especiales en Redis keys
    String sanitized = identifier.replaceAll("[^a-zA-Z0-9._-]", "_");
    return String.format(
        "%s%s:%s:%s:%s", RATE_LIMIT_PREFIX, type, identifierType, sanitized, window);
  }
}
