package com.wallparisoft.ebillbilling.dto.permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO para actualización masiva de features de planes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdatePlansRequestDto {

  @NotEmpty(message = "La lista de actualizaciones no puede estar vacía")
  @Valid
  private List<PlanFeatureUpdateDto> updates;
}
