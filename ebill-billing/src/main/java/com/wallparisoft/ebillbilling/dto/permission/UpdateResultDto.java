package com.wallparisoft.ebillbilling.dto.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO que representa el resultado de una actualización individual. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateResultDto {

  private String identifier; // planCode o featureCode
  private Boolean success;
  private String message;
  private String error;
}
