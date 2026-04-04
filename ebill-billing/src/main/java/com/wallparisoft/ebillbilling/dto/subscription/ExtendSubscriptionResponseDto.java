package com.wallparisoft.ebillbilling.dto.subscription;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Response DTO para extensión de suscripción Usado en el endpoint POST
 * /api/subscriptions/company/{idCompany}/extend
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtendSubscriptionResponseDto {

  Boolean success;
  String message;

  LocalDateTime previousExpiryDate;
  LocalDateTime newExpiryDate;
  Integer extensionMonths;

  SubscriptionResponseDto subscription;
}
