package com.wallparisoft.authentication.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for subscription response from ebill-billing service Represents the current subscription
 * state of a company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionResponseDto {

  private Long idSubscription;
  private Long idCompany;
  
  @JsonProperty("plan")
  private PlanDto plan;
  
  @JsonProperty("subscriptionStatus")
  private String status; // ACTIVE, EXPIRED, SUSPENDED, CANCELLED
  
  private LocalDateTime startDate;
  
  @JsonProperty("expiresAt")
  private LocalDateTime endDate;
  
  private List<String> features;
  
  @JsonProperty("plan")
  public void setPlan(PlanDto plan) {
    this.plan = plan;
    if (plan != null) {
      this.planCode = plan.getCode();
      this.planName = plan.getName();
      this.isActive = plan.getIsActive();
    }
  }
  
  // Fields extracted from plan object
  private String planCode;
  private String planName;
  private Boolean isActive;
  
  @JsonProperty("documentLimitYearly")
  private Integer invoiceLimit;
  
  @JsonProperty("currentYearUsage")
  private Integer invoiceUsed;
  
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PlanDto {
    private Long idPlan;
    private String code;
    private String name;
    private String description;
    private Boolean isActive;
    private Integer documentLimitYearly;
  }

  /** Check if subscription is active and not expired */
  public boolean isValidSubscription() {
    return Boolean.TRUE.equals(isActive)
        && "ACTIVE".equalsIgnoreCase(status)
        && (endDate == null || endDate.isAfter(LocalDateTime.now()));
  }

  /**
   * Normalize plan code to match database values
   * Maps common plan code variations to standard codes
   */
  private String normalizePlanCode(String code) {
    if (code == null) {
      return null;
    }
    // Map "PRO" to "PROFESSIONAL" to match database values
    if ("PRO".equalsIgnoreCase(code)) {
      return "PROFESSIONAL";
    }
    return code.toUpperCase();
  }

  /** Check if plan includes a specific feature */
  public boolean hasPlanFeature(String[] requiredPlans) {
    if (requiredPlans == null || requiredPlans.length == 0) {
      return true;
    }
    if (planCode == null) {
      return false;
    }
    
    // Normalize the user's plan code
    String normalizedPlanCode = normalizePlanCode(planCode);
    
    for (String plan : requiredPlans) {
      // Normalize required plan code and compare
      String normalizedRequiredPlan = normalizePlanCode(plan);
      if (normalizedPlanCode.equalsIgnoreCase(normalizedRequiredPlan)) {
        return true;
      }
    }
    return false;
  }

  /** Check if invoice limit is exceeded */
  public boolean hasReachedInvoiceLimit() {
    if (invoiceLimit == null || invoiceLimit == -1) {
      return false; // unlimited
    }
    if (invoiceUsed == null) {
      return false;
    }
    return invoiceUsed >= invoiceLimit;
  }
}
