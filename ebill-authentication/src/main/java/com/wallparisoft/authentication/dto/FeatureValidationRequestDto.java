package com.wallparisoft.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureValidationRequestDto {

  @NotBlank(message = "Feature code is required")
  private String featureCode;

  @NotNull(message = "Company ID is required")
  private Long idCompany;

  @NotNull(message = "User ID is required")
  private Long idUser;
}
