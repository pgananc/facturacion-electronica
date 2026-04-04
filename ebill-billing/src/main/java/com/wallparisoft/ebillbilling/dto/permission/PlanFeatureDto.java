package com.wallparisoft.ebillbilling.dto.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO que representa las features de un plan de suscripción. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanFeatureDto {

  /** Código del plan (FREE, BASIC, PROFESSIONAL, etc.) */
  private String planCode;

  /** Nombre del plan */
  private String planName;

  /** Features disponibles en el plan */
  private FeatureSetDto features;
}
