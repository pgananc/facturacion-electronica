package com.wallparisoft.authentication.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for feature permission details */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeaturePermissionDto {

  private String featureCode;
  private String featureName;
  private String description;
  private String module;
  private List<Integer> allowedRoles;
  private List<String> requiredPlans;
  private Boolean isActive;
}
