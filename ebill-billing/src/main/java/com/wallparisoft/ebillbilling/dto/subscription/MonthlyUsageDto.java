package com.wallparisoft.ebillbilling.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyUsageDto {
  private String month; // YYYY-MM
  private Integer invoices;
  private Integer creditNotes;
  private Integer debitNotes;
  private Integer retentions;
  private Integer guides;
  private Integer total;
}
