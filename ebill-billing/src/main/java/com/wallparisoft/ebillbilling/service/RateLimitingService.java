package com.wallparisoft.ebillbilling.service;

import com.wallparisoft.ebillbilling.ratelimit.RateLimitType;
import com.wallparisoft.ebillbilling.ratelimit.dto.RateLimitInfo;

/** Servicio para gestionar rate limiting. */
public interface RateLimitingService {

  /**
   * Verifica si el request puede proceder según los límites configurados.
   *
   * @param token Token JWT del usuario (puede ser null)
   * @param ipAddress Dirección IP del cliente
   * @param rateLimitType Tipo de rate limiting (QUERY o DOWNLOAD)
   * @throws com.wallparisoft.ebillbilling.exception.RateLimitExceededException Si se excede algún
   *     límite
   */
  void checkRateLimit(String token, String ipAddress, RateLimitType rateLimitType);

  /**
   * Obtiene información sobre los límites actuales sin lanzar excepción.
   *
   * @param token Token JWT del usuario (puede ser null)
   * @param ipAddress Dirección IP del cliente
   * @param rateLimitType Tipo de rate limiting (QUERY o DOWNLOAD)
   * @return Información sobre los límites actuales
   */
  RateLimitInfo getRateLimitInfo(String token, String ipAddress, RateLimitType rateLimitType);
}
