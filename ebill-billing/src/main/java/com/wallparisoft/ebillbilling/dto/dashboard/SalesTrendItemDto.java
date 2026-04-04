package com.wallparisoft.ebillbilling.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTrendItemDto {
  private LocalDate date;
  private String dayOfWeek;
  private BigDecimal amount;
  private Integer invoiceCount;
}
