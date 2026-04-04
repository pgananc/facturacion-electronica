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
public class PendingCollectionDto {
  private BigDecimal amount;
  private Integer invoiceCount;
  private Integer overdueCount;
}
