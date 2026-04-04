package com.wallparisoft.authentication.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for role permissions information */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionsDto {

  private Integer roleId;
  private String roleName;
  private List<FeaturePermissionDto> permissions;
  private Integer totalFeatures;
}
