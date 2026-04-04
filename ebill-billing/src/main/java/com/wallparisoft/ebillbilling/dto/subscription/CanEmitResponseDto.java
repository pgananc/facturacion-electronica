package com.wallparisoft.ebillbilling.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CanEmitResponseDto {
  private Boolean allowed;
  private Integer currentUsage;
  private Integer limit;
  private Integer remaining;
  private String message;
  private String reason; // LIMIT_REACHED, EXPIRED, FEATURE_NOT_AVAILABLE, ALLOWED
}
