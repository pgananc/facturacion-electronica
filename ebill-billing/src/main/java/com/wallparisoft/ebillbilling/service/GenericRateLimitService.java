package com.wallparisoft.ebillbilling.service;

/**
 * Interfaz para el servicio genérico de rate limiting. Soporta rate limiting por múltiples
 * dimensiones (TOKEN, IP) para diferentes endpoints.
 */
public interface GenericRateLimitService {

  /** Enum que define las dimensiones de rate limiting disponibles. */
  enum RateLimitDimension {
    TOKEN,
    IP
  }

  /**
   * Valida si se puede realizar una solicitud según el rate limit configurado.
   *
   * @param endpoint Código del endpoint (ej: "DOCUMENTS_QUERY", "MAIL_SEND")
   * @param dimension Dimensión del rate limit (TOKEN, IP)
   * @param identifier Identificador único (token, IP)
   * @return true si puede realizar la solicitud, false si excedió el límite
   */
  boolean canMakeRequest(String endpoint, RateLimitDimension dimension, String identifier);

  /**
   * Incrementa el contador de rate limiting para una solicitud.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @param identifier Identificador único
   */
  void incrementRequest(String endpoint, RateLimitDimension dimension, String identifier);

  /**
   * Obtiene el tiempo restante hasta el próximo intento permitido.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @param identifier Identificador único
   * @return Tiempo restante en segundos
   */
  long getTimeUntilNextAttempt(String endpoint, RateLimitDimension dimension, String identifier);

  /**
   * Obtiene el número de intentos actuales.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @param identifier Identificador único
   * @return Número de intentos actuales
   */
  int getCurrentAttempts(String endpoint, RateLimitDimension dimension, String identifier);

  /**
   * Obtiene el máximo de solicitudes permitidas desde params.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @return Máximo de solicitudes permitidas
   */
  int getMaxRequests(String endpoint, RateLimitDimension dimension);

  /**
   * Obtiene la ventana de tiempo en minutos desde params.
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @return Ventana de tiempo en minutos
   */
  int getWindowMinutes(String endpoint, RateLimitDimension dimension);

  /**
   * Limpia el contador de rate limiting (útil para testing o casos especiales).
   *
   * @param endpoint Código del endpoint
   * @param dimension Dimensión del rate limit
   * @param identifier Identificador único
   */
  void clearAttempts(String endpoint, RateLimitDimension dimension, String identifier);
}
