package com.wallparisoft.ebillbilling.exception;

import java.time.LocalDateTime;
import lombok.Getter;

/**
 * Excepción lanzada cuando hay un conflicto con el estado actual del recurso. Debe retornar HTTP
 * 409 Conflict.
 */
@Getter
public class ConflictException extends RuntimeException {

  private final Long idCompany;
  private final Long existingSubscriptionId;
  private final LocalDateTime expiresAt;

  public ConflictException(String message) {
    super(message);
    this.idCompany = null;
    this.existingSubscriptionId = null;
    this.expiresAt = null;
  }

  public ConflictException(
      String message, Long idCompany, Long existingSubscriptionId, LocalDateTime expiresAt) {
    super(message);
    this.idCompany = idCompany;
    this.existingSubscriptionId = existingSubscriptionId;
    this.expiresAt = expiresAt;
  }
}
