package com.wallparisoft.ebillbilling.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** DTO para la búsqueda de guías de remisión por compañía con filtros opcionales. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuideSearchByCompanyDto {

  /** ID de la compañía (obligatorio) */
  @NotNull(message = "idCompany es obligatorio")
  Long idCompany;

  /** Número de guía (opcional) */
  String guideNumber;
}
