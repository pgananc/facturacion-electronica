package com.wallparisoft.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureValidationResponseDto {

  private Boolean allowed;
  private String reason;
  private String message;
  private String upgradeUrl;

  // Convenience factory methods for common responses
  public static FeatureValidationResponseDto allowed(String message) {
    return FeatureValidationResponseDto.builder().allowed(true).message(message).build();
  }

  public static FeatureValidationResponseDto denied(String reason, String message) {
    return FeatureValidationResponseDto.builder()
        .allowed(false)
        .reason(reason)
        .message(message)
        .build();
  }

  public static FeatureValidationResponseDto deniedWithUpgrade(
      String reason, String message, String upgradeUrl) {
    return FeatureValidationResponseDto.builder()
        .allowed(false)
        .reason(reason)
        .message(message)
        .upgradeUrl(upgradeUrl)
        .build();
  }

  // Common denial reasons
  public static final String REASON_NO_ROLE = "NO_ROLE";
  public static final String REASON_NO_SUBSCRIPTION = "NO_SUBSCRIPTION";
  public static final String REASON_LIMIT_EXCEEDED = "LIMIT_EXCEEDED";
  public static final String REASON_EXPIRED = "EXPIRED";
  public static final String REASON_FEATURE_NOT_FOUND = "FEATURE_NOT_FOUND";
  public static final String REASON_INACTIVE_FEATURE = "INACTIVE_FEATURE";
  public static final String REASON_PLAN_NOT_ALLOWED = "PLAN_NOT_ALLOWED";
}
