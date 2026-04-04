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
public class KpiDto {
  private BigDecimal amount;
  private Integer invoiceCount;
  private Double percentageChange;
  private Double goalPercentage; // Opcional, para futuro
}
