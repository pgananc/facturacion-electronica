package com.wallparisoft.ebillbilling.dto.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO para cancelar una suscripción Usado en el endpoint DELETE
 * /api/subscriptions/company/{idCompany}/cancel
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancelSubscriptionRequestDto {

  @NotBlank(message = "La razón de cancelación es requerida")
  String reason; // PAYMENT_FAILURE, USER_REQUEST, VIOLATION, OTHER

  String notes;

  @NotNull(message = "Debe especificar si la cancelación es inmediata")
  @Builder.Default
  Boolean cancelImmediately = true;
}
