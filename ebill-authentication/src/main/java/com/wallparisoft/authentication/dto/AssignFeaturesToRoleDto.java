package com.wallparisoft.authentication.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for assigning multiple features to a role */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignFeaturesToRoleDto {

  @NotEmpty(message = "Feature codes list cannot be empty")
  private List<String> featureCodes;
}
