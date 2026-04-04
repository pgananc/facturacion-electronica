package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.entity.Params;
import com.wallparisoft.authentication.repository.IParamsRepository;
import com.wallparisoft.ebill.auth.util.UtilsRedis;
import com.wallparisoft.ebill.utils.enums.ParmsAuthEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientTokenRateLimitService {

  private final UtilsRedis utilsRedis;
  private final IParamsRepository paramsRepository;

  // Verificar si puede solicitar token
  public boolean canRequestToken(String email, String identification, boolean status) {
    String key = buildRateLimitKey(email, identification, status);
    String attempts = utilsRedis.getKey(key);

    if (attempts == null) {
      return true; // No hay intentos previos para este status
    }

    int currentAttempts = Integer.parseInt(attempts);
    int maxAttempts = getMaxAttempts();
    return currentAttempts < maxAttempts;
  }

  // Incrementar contador de intentos
  public void incrementAttempts(String email, String identification, boolean status) {
    String key = buildRateLimitKey(email, identification, status);

    // Incrementar contador
    Long attempts = utilsRedis.incrementKey(key);

    // Si es el primer intento, establecer TTL
    if (attempts != null && attempts == 1) {
      int cooldownMinutes = getCooldownMinutes();
      long ttlSeconds = cooldownMinutes * 60; // Convertir minutos a segundos
      utilsRedis.setKeyWithTTL(key, "1", ttlSeconds);
      log.debug("TTL establecido para rate limiting: {} - TTL: {} minutos", key, cooldownMinutes);
    }
  }

  // Obtener tiempo restante hasta próximo intento
  public long getTimeUntilNextAttempt(String email, String identification, boolean status) {
    String key = buildRateLimitKey(email, identification, status);
    return utilsRedis.getTTL(key); // Retorna segundos restantes
  }

  // Obtener número de intentos actuales
  public int getCurrentAttempts(String email, String identification, boolean status) {
    String key = buildRateLimitKey(email, identification, status);
    String attempts = utilsRedis.getKey(key);
    return attempts != null ? Integer.parseInt(attempts) : 0;
  }

  // Obtener máximo de intentos desde params
  public int getMaxAttempts() {
    try {
      Params param =
          paramsRepository.findParamsByCodeParam(
              ParmsAuthEnum.COD007_CLIENT_TOKEN_MAX_ATTEMPTS.getCode());
      return Integer.parseInt(param.getValueParam());
    } catch (Exception e) {
      log.warn("Error al obtener max attempts, usando valor por defecto: 3");
      return 3; // Valor por defecto
    }
  }

  // Obtener minutos de cooldown desde params
  public int getCooldownMinutes() {
    try {
      Params param =
          paramsRepository.findParamsByCodeParam(
              ParmsAuthEnum.COD008_CLIENT_TOKEN_COOLDOWN_MINUTES.getCode());
      return Integer.parseInt(param.getValueParam());
    } catch (Exception e) {
      log.warn("Error al obtener cooldown minutes, usando valor por defecto: 120");
      return 120; // Valor por defecto (120 minutos = 2 horas)
    }
  }

  // Obtener prefijo de clave Redis desde params
  public String getRedisKeyPrefix() {
    try {
      Params param =
          paramsRepository.findParamsByCodeParam(
              ParmsAuthEnum.COD009_CLIENT_TOKEN_REDIS_KEY_PREFIX.getCode());
      return param.getValueParam();
    } catch (Exception e) {
      log.warn("Error al obtener redis key prefix, usando valor por defecto");
      return "client_token_attempts"; // Valor por defecto
    }
  }

  // Construir clave de rate limiting
  private String buildRateLimitKey(String email, String identification, boolean status) {
    String prefix = getRedisKeyPrefix();
    return prefix + ":" + email + ":" + identification + ":" + status;
  }

  // Verificar si existe la clave en Redis
  public boolean hasAttemptsRecord(String email, String identification, boolean status) {
    String key = buildRateLimitKey(email, identification, status);
    return utilsRedis.hasKey(key);
  }

  // Limpiar intentos (útil para testing o casos especiales)
  public void clearAttempts(String email, String identification, boolean status) {
    String key = buildRateLimitKey(email, identification, status);
    utilsRedis.deleteKey(key);
    log.info(
        "Intentos de rate limiting limpiados para: {} - {} - status: {}",
        email,
        identification,
        status);
  }

  /**
   * Decrementa el contador de intentos cuando se valida exitosamente un token
   *
   * @param email el email del cliente
   * @param identification la identificación del cliente
   * @param status el estado del token (true para activos)
   */
  public void decrementAttempts(String email, String identification, boolean status) {
    String key = buildRateLimitKey(email, identification, status);

    // Obtener valor actual
    String currentAttempts = utilsRedis.getKey(key);

    if (currentAttempts != null) {
      int attempts = Integer.parseInt(currentAttempts);

      // Solo decrementar si hay intentos
      if (attempts > 0) {
        attempts--;

        if (attempts == 0) {
          // Si llega a 0, eliminar la clave
          utilsRedis.deleteKey(key);
          log.debug("Contador de rate limiting eliminado para: {}", key);
        } else {
          // Actualizar con el nuevo valor
          int cooldownMinutes = getCooldownMinutes();
          long ttlSeconds = cooldownMinutes * 60;
          utilsRedis.setKeyWithTTL(key, String.valueOf(attempts), ttlSeconds);
          log.debug("Contador decrementado para: {} - Nuevo valor: {}", key, attempts);
        }
      }
    }
  }
}
