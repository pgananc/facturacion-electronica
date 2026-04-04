package com.wallparisoft.ebillbilling.dto.subscription;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class YearlyUsageDto {
  private Integer year;
  private Integer total;
  private Integer limit;
  private BigDecimal percentUsed;
  private Integer remaining;
}
