package com.wallparisoft.ebillbilling.dto.subscription;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO para cancelación de suscripción Usado en el endpoint DELETE
 * /api/subscriptions/company/{idCompany}/cancel
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancelSubscriptionResponseDto {

  Boolean success;
  String message;

  LocalDateTime cancelledAt;
  String cancelReason;

  SubscriptionCancelledDto subscription;

  /** DTO simplificado de la suscripción cancelada */
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class SubscriptionCancelledDto {
    Long idSubscription;
    Long idCompany;
    String subscriptionStatus;
    LocalDateTime cancelledAt;
    String cancelReason;
  }
}
