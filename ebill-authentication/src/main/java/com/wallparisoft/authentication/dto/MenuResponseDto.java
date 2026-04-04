package com.wallparisoft.authentication.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuResponseDto {
  private Integer id;
  private String name;
  private String icon;
  private String url;
  private String featureCode;
  private FeaturePermissionDto feature; // Objeto completo del feature permission
  private Integer parentId;
  private Boolean active;
  private Integer order;
  private List<MenuResponseDto> subMenus;
}
