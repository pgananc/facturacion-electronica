package com.wallparisoft.ebillbilling.dto.dashboard;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeDashboardDto {
  private KpiSummaryDto kpis;
  private List<SalesTrendItemDto> salesTrend;
  private TrendSummaryDto trendSummary;
  private List<RecentInvoiceDto> recentInvoices;
  private List<AlertDto> alerts;
}
