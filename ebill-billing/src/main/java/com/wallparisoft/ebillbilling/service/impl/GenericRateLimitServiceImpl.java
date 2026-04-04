package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.entity.Params;
import com.wallparisoft.ebillbilling.repository.IParamsRepo;
import com.wallparisoft.ebillbilling.service.GenericRateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Servicio genérico para manejar rate limiting con soporte para múltiples dimensiones. Soporta rate
 * limiting por TOKEN e IP para diferentes endpoints.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenericRateLimitServiceImpl implements GenericRateLimitService {

  private final StringRedisTemplate redisTemplate;
  private final IParamsRepo paramsRepository;

  /**
   * Valida si se puede realizar una solicitud según el rate limit configurado.
   *
   * @param endpoint Código del endpoint (ej: "DOCUMENTS_QUERY", "MAIL_SEND")
   * @param dimension Dimensión del rate limit (TOKEN, IP)
   * @param identifier Identificador único (token, IP)
   * @return true si puede realizar la solicitud, false si excedió el límite
   */
  @Override
  public boolean canMakeRequest(String endpoint, RateLimitDimension dimension, String identifier) {
    // Verificar si el rate limiting está habilitado globalmente
    if (!isRateLimitingEnabled()) {
      log.debug(
          "Rate limiting deshabilitado globalmente, permitiendo solicitud para {} - {} - {}",
          endpoint,
          dimension,
          identifier);
      return true; // Permite todas las solicitudes si está deshabilitado
    }

    String key = buildRateLimitKey(endpoint, dimension, identifier);
    String attempts = redisTemplate.opsForValue().get(key);

    if (attempts == null) {
      return true; // No hay intentos previos
    }

    int currentAttempts = Integer.parseInt(attempts);
    int maxAttempts = getMaxRequests(endpoint, dimension);
    return currentAttempts < maxAttempts;
  }

  /**
   * Incrementa el contador de rate limiting para una solicitud.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @param identifier Identificador único
   */
  @Override
  public void incrementRequest(String endpoint, RateLimitDimension dimension, String identifier) {
    // Verificar si el rate limiting está habilitado globalmente
    if (!isRateLimitingEnabled()) {
      log.debug(
          "Rate limiting deshabilitado globalmente, no incrementando contador para {} - {} - {}",
          endpoint,
          dimension,
          identifier);
      return; // No incrementa contadores si está deshabilitado
    }

    String key = buildRateLimitKey(endpoint, dimension, identifier);

    // Incrementar contador
    Long attempts = redisTemplate.opsForValue().increment(key);

    // Si es el primer intento, establecer TTL
    if (attempts != null && attempts == 1) {
      int windowMinutes = getWindowMinutes(endpoint, dimension);
      long ttlSeconds = windowMinutes * 60; // Convertir minutos a segundos
      redisTemplate.expire(key, java.time.Duration.ofSeconds(ttlSeconds));
      log.debug("TTL establecido para rate limiting: {} - TTL: {} minutos", key, windowMinutes);
    }
  }

  /**
   * Obtiene el tiempo restante hasta el próximo intento permitido.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @param identifier Identificador único
   * @return Tiempo restante en segundos
   */
  @Override
  public long getTimeUntilNextAttempt(
      String endpoint, RateLimitDimension dimension, String identifier) {
    String key = buildRateLimitKey(endpoint, dimension, identifier);
    Long ttl = redisTemplate.getExpire(key);
    return ttl != null && ttl > 0 ? ttl : 0;
  }

  /**
   * Obtiene el número de intentos actuales.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @param identifier Identificador único
   * @return Número de intentos actuales
   */
  @Override
  public int getCurrentAttempts(String endpoint, RateLimitDimension dimension, String identifier) {
    String key = buildRateLimitKey(endpoint, dimension, identifier);
    String attempts = redisTemplate.opsForValue().get(key);
    return attempts != null ? Integer.parseInt(attempts) : 0;
  }

  /**
   * Obtiene el máximo de solicitudes permitidas desde params.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @return Máximo de solicitudes permitidas
   */
  @Override
  public int getMaxRequests(String endpoint, RateLimitDimension dimension) {
    try {
      String codeParam = buildMaxRequestsCodeParam(endpoint, dimension);
      java.util.Optional<Params> paramOpt = paramsRepository.findByCodeParam(codeParam);
      if (paramOpt.isPresent() && paramOpt.get().getValueParam() != null) {
        return Integer.parseInt(paramOpt.get().getValueParam().trim());
      }
    } catch (Exception e) {
      log.warn(
          "Error al obtener max requests para {} - {}, usando valor por defecto: 100",
          endpoint,
          dimension,
          e);
    }
    // Valores por defecto conservadores
    return 100;
  }

  /**
   * Obtiene la ventana de tiempo en minutos desde params.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @return Ventana de tiempo en minutos
   */
  @Override
  public int getWindowMinutes(String endpoint, RateLimitDimension dimension) {
    try {
      String codeParam = buildWindowMinutesCodeParam(endpoint, dimension);
      java.util.Optional<Params> paramOpt = paramsRepository.findByCodeParam(codeParam);
      if (paramOpt.isPresent() && paramOpt.get().getValueParam() != null) {
        return Integer.parseInt(paramOpt.get().getValueParam().trim());
      }
    } catch (Exception e) {
      log.warn(
          "Error al obtener window minutes para {} - {}, usando valor por defecto: 60",
          endpoint,
          dimension,
          e);
    }
    // Valor por defecto: 60 minutos (1 hora)
    return 60;
  }

  /**
   * Limpia el contador de rate limiting (útil para testing o casos especiales).
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @param identifier Identificador único
   */
  @Override
  public void clearAttempts(String endpoint, RateLimitDimension dimension, String identifier) {
    String key = buildRateLimitKey(endpoint, dimension, identifier);
    redisTemplate.delete(key);
    log.info(
        "Intentos de rate limiting limpiados para: {} - {} - {}", endpoint, dimension, identifier);
  }

  /**
   * Construye la clave de Redis para rate limiting.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @param identifier Identificador único
   * @return Clave de Redis
   */
  private String buildRateLimitKey(
      String endpoint, RateLimitDimension dimension, String identifier) {
    return String.format("rate_limit:%s:%s:%s", endpoint, dimension.name(), identifier);
  }

  /**
   * Construye el código del parámetro para MAX_REQUESTS.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @return Código del parámetro
   */
  private String buildMaxRequestsCodeParam(String endpoint, RateLimitDimension dimension) {
    String dimensionSuffix = getDimensionSuffix(dimension);
    return String.format("%s_RATE_LIMIT_%s_MAX_REQUESTS", endpoint, dimensionSuffix);
  }

  /**
   * Construye el código del parámetro para WINDOW_MINUTES.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @return Código del parámetro
   */
  private String buildWindowMinutesCodeParam(String endpoint, RateLimitDimension dimension) {
    String dimensionSuffix = getDimensionSuffix(dimension);
    return String.format("%s_RATE_LIMIT_%s_WINDOW_MINUTES", endpoint, dimensionSuffix);
  }

  /**
   * Obtiene el sufijo de dimensión para construir códigos de parámetros.
   *
   * @param dimension Dimensión del rate limit
   * @return Sufijo (TOKEN, IP)
   */
  private String getDimensionSuffix(RateLimitDimension dimension) {
    return dimension.name();
  }

  /**
   * Verifica si el rate limiting está habilitado globalmente. Consulta el parámetro
   * RATE_LIMITING_ENABLED de la base de datos.
   *
   * @return true si está habilitado, false si está deshabilitado. Por defecto retorna true
   *     (habilitado) si hay error o el parámetro no existe.
   */
  private boolean isRateLimitingEnabled() {
    try {
      java.util.Optional<Params> paramOpt =
          paramsRepository.findByCodeParam("RATE_LIMITING_ENABLED");
      if (paramOpt.isPresent() && paramOpt.get().getValueParam() != null) {
        boolean enabled = Boolean.parseBoolean(paramOpt.get().getValueParam().trim());
        if (!enabled) {
          log.warn(
              "⚠️ Rate limiting está DESHABILITADO globalmente - todas las solicitudes serán permitidas");
        }
        return enabled;
      }
    } catch (Exception e) {
      log.warn(
          "Error al verificar si rate limiting está habilitado, asumiendo habilitado por defecto",
          e);
    }
    // Por defecto: habilitado (comportamiento seguro)
    return true;
  }
}
