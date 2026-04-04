package com.wallparisoft.ebillbilling.dto.subscription;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO para extender la vigencia de una suscripción Usado en el endpoint POST
 * /api/subscriptions/company/{idCompany}/extend
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtendSubscriptionRequestDto {

  @NotNull(message = "El número de meses de extensión es requerido")
  @Min(value = 1, message = "La extensión debe ser de al menos 1 mes")
  Integer extensionMonths;

  LocalDateTime paymentDate;

  BigDecimal paymentAmount;

  String paymentReference;

  String notes;
}
