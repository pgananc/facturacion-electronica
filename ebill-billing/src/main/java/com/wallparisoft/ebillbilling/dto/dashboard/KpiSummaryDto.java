package com.wallparisoft.ebillbilling.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiSummaryDto {
  private KpiDto todaySales;
  private KpiDto weekSales;
  private KpiDto monthSales;
  private PendingCollectionDto pendingCollection;
}
