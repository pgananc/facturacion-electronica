package com.wallparisoft.ebillbilling.dto.subscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResponseDto {
  private Long idSubscription;
  private Long idCompany;
  private SubscriptionPlanDto plan;
  private String subscriptionStatus;
  private LocalDateTime startDate;
  private LocalDateTime expiresAt;
  private Integer currentYearUsage;
  private LocalDateTime lastResetDate;

  // Calculated fields
  private BigDecimal percentUsed;
  private Integer documentsRemaining;
  private Boolean isExpired;
  private Boolean isLimitReached;

  // Payment info
  private String paymentMethod;
  private LocalDateTime lastPaymentDate;
  private BigDecimal lastPaymentAmount;
  private String paymentReference;
  private String notes;
}
