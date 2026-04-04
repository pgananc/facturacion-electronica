package com.wallparisoft.ebillbilling.dto.permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para actualizar las features de un plan específico. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeatureUpdateDto {

  @NotBlank(message = "El código del plan es requerido")
  private String planCode;

  @Valid private FeatureSetDto features;
}
