package com.wallparisoft.ebillbilling.dto.dashboard;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendSummaryDto {
  private BigDecimal average;
  private BigDecimal total;
}
