package com.wallparisoft.ebillbilling.dto.subscription;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsageResponseDto {
  private MonthlyUsageDto currentMonth;
  private YearlyUsageDto currentYear;
  private List<MonthlyUsageDto> lastMonths;
}
