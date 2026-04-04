package com.wallparisoft.ebillbilling.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentInvoiceDto {
  private Long idInvoice;
  private String invoiceNumber;
  private String clientName;
  private String clientIdentification;
  private BigDecimal totalAmount;
  private LocalDateTime issueDate;
  private String authorizationStatus;
  private String paymentStatus;
}
