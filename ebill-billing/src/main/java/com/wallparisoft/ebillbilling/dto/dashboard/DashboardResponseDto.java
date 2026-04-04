package com.wallparisoft.ebillbilling.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDto {
  private String status;
  private String message;
  private WelcomeDashboardDto data;
}
