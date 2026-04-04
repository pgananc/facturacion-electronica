package com.wallparisoft.ebillbilling.dto.subscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * DTO para listar suscripciones en el panel de administración Usado en el endpoint GET
 * /api/subscriptions
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionListDto {

  Long idSubscription;
  Long idCompany;
  String companyName; // Opcional: nombre de la empresa si se hace join

  // Plan básico
  PlanBasicDto plan;

  String subscriptionStatus; // ACTIVE, EXPIRED, CANCELLED, SUSPENDED
  LocalDateTime startDate;
  LocalDateTime expiresAt;

  // Uso
  Integer currentYearUsage;
  BigDecimal percentUsed;
  Integer documentsRemaining;

  // Estados
  Boolean isExpired;
  Boolean isLimitReached;

  // Cancelación
  LocalDateTime cancelledAt;
  String cancelReason;

  /** DTO simplificado del plan para el listado */
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class PlanBasicDto {
    Long idPlan;
    String code;
    String name;
    Integer documentLimitYearly;
    BigDecimal priceAnnual;
    BigDecimal priceMonthly;
  }
}
