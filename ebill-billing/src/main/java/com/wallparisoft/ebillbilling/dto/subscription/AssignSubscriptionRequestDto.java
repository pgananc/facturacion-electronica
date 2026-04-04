package com.wallparisoft.ebillbilling.dto.subscription;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO para asignar una nueva suscripción a una empresa. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignSubscriptionRequestDto {

  @NotNull(message = "El ID de la empresa es obligatorio")
  private Long idCompany;

  @NotBlank(message = "El código del plan es obligatorio")
  @Pattern(
      regexp = "^(FREE|BASIC|PROFESSIONAL|ENTERPRISE)$",
      message = "Plan inválido. Debe ser: FREE, BASIC, PROFESSIONAL o ENTERPRISE")
  private String planCode;

  @NotNull(message = "La fecha de inicio es obligatoria")
  @FutureOrPresent(message = "La fecha debe ser hoy o futura")
  private LocalDate startDate;

  @NotNull(message = "La duración es obligatoria")
  @Min(value = 1, message = "La duración debe ser al menos 1 mes")
  @Max(value = 60, message = "La duración máxima es 60 meses")
  private Integer durationMonths;
}
