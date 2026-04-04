package com.wallparisoft.authentication.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for updating allowed roles of a feature */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeatureRolesDto {

  private List<Integer> allowedRoles;
}
