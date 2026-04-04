package com.wallparisoft.ebillbilling.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpgradeResponseDto {
  private Boolean success;
  private String message;
  private SubscriptionResponseDto subscription;
}
