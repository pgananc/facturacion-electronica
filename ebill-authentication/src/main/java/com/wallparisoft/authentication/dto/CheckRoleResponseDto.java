package com.wallparisoft.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckRoleResponseDto {

  private Boolean allowed;
  private String message;
  private String featureCode;
  private String featureName;
  private String reason;

  // Convenience factory methods
  public static CheckRoleResponseDto allowed(String featureCode, String featureName) {
    return CheckRoleResponseDto.builder()
        .allowed(true)
        .featureCode(featureCode)
        .featureName(featureName)
        .message("Access granted to feature")
        .build();
  }

  public static CheckRoleResponseDto denied(String featureCode, String reason, String message) {
    return CheckRoleResponseDto.builder()
        .allowed(false)
        .featureCode(featureCode)
        .reason(reason)
        .message(message)
        .build();
  }
}
