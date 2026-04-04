package com.wallparisoft.ebill.auth.util;

import com.wallparisoft.ebill.auth.util.model.RedisEntity;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UtilsRedis {
  final StringRedisTemplate stringRedisTemplate;

  public String registerKey(RedisEntity redis) {
    ValueOperations<String, String> objectRedis = stringRedisTemplate.opsForValue();
    objectRedis.set(redis.getKey(), redis.getValue(), Duration.ofMinutes(redis.getDuration()));

    return redis.getKey();
  }

  public boolean checkExistRegister(String key) {
    return stringRedisTemplate.hasKey(key);
  }

  public String getValue(String key) {
    ValueOperations<String, String> objectRedis = stringRedisTemplate.opsForValue();
    return objectRedis.get(key);
  }

  public boolean removeKey(String key) {
    return stringRedisTemplate.delete(key);
  }

  // ========== NUEVOS MÉTODOS PARA RATE LIMITING ==========

  /**
   * Obtiene el valor de una clave específica en Redis
   *
   * @param key La clave a consultar
   * @return El valor de la clave o null si no existe
   */
  public String getKey(String key) {
    try {
      ValueOperations<String, String> objectRedis = stringRedisTemplate.opsForValue();
      return objectRedis.get(key);
    } catch (Exception e) {
      log.error("Error al obtener clave Redis: {} - Error: {}", key, e.getMessage());
      return null;
    }
  }

  /**
   * Incrementa el valor de una clave en Redis de forma atómica
   *
   * @param key La clave a incrementar
   * @return El nuevo valor después del incremento
   */
  public Long incrementKey(String key) {
    try {
      ValueOperations<String, String> objectRedis = stringRedisTemplate.opsForValue();
      return objectRedis.increment(key);
    } catch (Exception e) {
      log.error("Error al incrementar clave Redis: {} - Error: {}", key, e.getMessage());
      return 0L;
    }
  }

  /**
   * Establece una clave con valor y TTL específico en Redis
   *
   * @param key La clave a establecer
   * @param value El valor a asignar
   * @param ttlSeconds Tiempo de vida en segundos
   */
  public void setKeyWithTTL(String key, String value, long ttlSeconds) {
    try {
      ValueOperations<String, String> objectRedis = stringRedisTemplate.opsForValue();
      objectRedis.set(key, value, Duration.ofSeconds(ttlSeconds));
      log.debug("Clave Redis establecida: {} = {} con TTL: {} segundos", key, value, ttlSeconds);
    } catch (Exception e) {
      log.error("Error al establecer clave Redis con TTL: {} - Error: {}", key, e.getMessage());
    }
  }

  /**
   * Obtiene el tiempo de vida restante de una clave en Redis
   *
   * @param key La clave a consultar
   * @return Tiempo restante en segundos, -1 si no tiene TTL, -2 si no existe
   */
  public long getTTL(String key) {
    try {
      return stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
    } catch (Exception e) {
      log.error("Error al obtener TTL de clave Redis: {} - Error: {}", key, e.getMessage());
      return -1L;
    }
  }

  /**
   * Verifica si una clave existe en Redis
   *
   * @param key La clave a verificar
   * @return true si la clave existe, false en caso contrario
   */
  public boolean hasKey(String key) {
    try {
      Boolean exists = stringRedisTemplate.hasKey(key);
      return exists != null && exists;
    } catch (Exception e) {
      log.error(
          "Error al verificar existencia de clave Redis: {} - Error: {}", key, e.getMessage());
      return false;
    }
  }

  /**
   * Establece una clave con valor en Redis sin TTL
   *
   * @param key La clave a establecer
   * @param value El valor a asignar
   */
  public void setKey(String key, String value) {
    try {
      ValueOperations<String, String> objectRedis = stringRedisTemplate.opsForValue();
      objectRedis.set(key, value);
      log.debug("Clave Redis establecida: {} = {}", key, value);
    } catch (Exception e) {
      log.error("Error al establecer clave Redis: {} - Error: {}", key, e.getMessage());
    }
  }

  /**
   * Elimina una clave específica de Redis
   *
   * @param key La clave a eliminar
   * @return true si la clave fue eliminada, false en caso contrario
   */
  public boolean deleteKey(String key) {
    try {
      Boolean deleted = stringRedisTemplate.delete(key);
      log.debug("Clave Redis eliminada: {} - Resultado: {}", key, deleted);
      return deleted != null && deleted;
    } catch (Exception e) {
      log.error("Error al eliminar clave Redis: {} - Error: {}", key, e.getMessage());
      return false;
    }
  }

  /**
   * Incrementa una clave y establece TTL si es la primera vez
   *
   * @param key La clave a incrementar
   * @param ttlSeconds TTL a establecer si es el primer incremento
   * @return El nuevo valor después del incremento
   */
  public Long incrementKeyWithTTL(String key, long ttlSeconds) {
    try {
      ValueOperations<String, String> objectRedis = stringRedisTemplate.opsForValue();
      Long newValue = objectRedis.increment(key);

      // Si es el primer incremento (valor = 1), establecer TTL
      if (newValue != null && newValue == 1) {
        stringRedisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        log.debug("TTL establecido para clave Redis: {} - TTL: {} segundos", key, ttlSeconds);
      }

      return newValue;
    } catch (Exception e) {
      log.error("Error al incrementar clave Redis con TTL: {} - Error: {}", key, e.getMessage());
      return 0L;
    }
  }

  /**
   * Establece una clave con valor y TTL en horas
   *
   * @param key La clave a establecer
   * @param value El valor a asignar
   * @param ttlHours Tiempo de vida en horas
   */
  public void setKeyWithTTLHours(String key, String value, long ttlHours) {
    try {
      ValueOperations<String, String> objectRedis = stringRedisTemplate.opsForValue();
      long ttlSeconds = ttlHours * 3600; // Convertir horas a segundos
      objectRedis.set(key, value, Duration.ofSeconds(ttlSeconds));
      log.debug("Clave Redis establecida: {} = {} con TTL: {} horas", key, value, ttlHours);
    } catch (Exception e) {
      log.error(
          "Error al establecer clave Redis con TTL en horas: {} - Error: {}", key, e.getMessage());
    }
  }

  /**
   * Obtiene el tiempo de vida restante de una clave en Redis en minutos
   *
   * @param key La clave a consultar
   * @return Tiempo restante en minutos, -1 si no tiene TTL, -2 si no existe
   */
  public long getTTLMinutes(String key) {
    try {
      long seconds = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
      return seconds > 0 ? seconds / 60 : seconds;
    } catch (Exception e) {
      log.error(
          "Error al obtener TTL en minutos de clave Redis: {} - Error: {}", key, e.getMessage());
      return -1L;
    }
  }

  public void registerPointSession(String username, String invoicePointId) {
    String tokenKey = "token:" + username;
    Long ttl = stringRedisTemplate.getExpire(tokenKey, TimeUnit.SECONDS);

    if (ttl > 0) {
      String key = "point-session:" + username;
      stringRedisTemplate.opsForValue().set(key, invoicePointId, ttl, TimeUnit.SECONDS);
    }
  }

  public String getPointSession(String username) {
    return stringRedisTemplate.opsForValue().get("point-session:" + username);
  }

  public void removePointSession(String username) {
    stringRedisTemplate.delete("point-session:" + username);
  }
}
