package com.wallparisoft.authentication.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for updating required plans of a feature */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeaturePlansDto {

  private List<String> requiredPlans;
}
