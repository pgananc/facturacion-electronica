package com.wallparisoft.ebillbilling.exception;

import com.wallparisoft.ebillbilling.ratelimit.dto.RateLimitInfo;
import java.time.LocalDateTime;

/** Excepción lanzada cuando se excede un límite de rate limiting. */
public class RateLimitExceededException extends RuntimeException {

  private final RateLimitInfo rateLimitInfo; // Mantener para compatibilidad con código existente
  private final LocalDateTime timestamp;
  private final long minutesUntilNextAttempt;
  private final int attemptsUsed;
  private final int maxAttempts;
  private final int cooldownMinutes;

  // Constructor para compatibilidad con código existente (AOP)
  public RateLimitExceededException(String message, RateLimitInfo rateLimitInfo) {
    super(message);
    this.rateLimitInfo = rateLimitInfo;
    this.timestamp = LocalDateTime.now();
    // Valores por defecto si no se proporcionan
    this.minutesUntilNextAttempt = 0;
    this.attemptsUsed = 0;
    this.maxAttempts = 0;
    this.cooldownMinutes = 0;
  }

  // Constructor nuevo con información completa (patrón del servicio de autenticación)
  public RateLimitExceededException(
      String message,
      long minutesUntilNextAttempt,
      int attemptsUsed,
      int maxAttempts,
      int cooldownMinutes) {
    super(message);
    this.rateLimitInfo = null; // No usar RateLimitInfo en el nuevo patrón
    this.timestamp = LocalDateTime.now();
    this.minutesUntilNextAttempt = minutesUntilNextAttempt;
    this.attemptsUsed = attemptsUsed;
    this.maxAttempts = maxAttempts;
    this.cooldownMinutes = cooldownMinutes;
  }

  public RateLimitInfo getRateLimitInfo() {
    return rateLimitInfo;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public long getMinutesUntilNextAttempt() {
    return minutesUntilNextAttempt;
  }

  public int getAttemptsUsed() {
    return attemptsUsed;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public int getCooldownMinutes() {
    return cooldownMinutes;
  }

  public long getSecondsUntilNextAttempt() {
    return minutesUntilNextAttempt * 60;
  }
}
