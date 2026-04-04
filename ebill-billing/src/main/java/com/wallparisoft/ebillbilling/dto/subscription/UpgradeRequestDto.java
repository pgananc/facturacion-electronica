package com.wallparisoft.ebillbilling.dto.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpgradeRequestDto {
  @NotBlank(message = "El código del plan es requerido")
  private String newPlanCode;

  private String paymentReference;

  @Positive(message = "El monto del pago debe ser mayor a cero")
  private BigDecimal paymentAmount;

  @NotNull(message = "La fecha de pago es requerida")
  private LocalDateTime paymentDate;

  private String notes;
}
