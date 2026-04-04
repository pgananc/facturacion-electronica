package com.wallparisoft.ebillbilling.dto.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO que representa el conjunto de features disponibles en un plan. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureSetDto {

  private Boolean hasApiAccess;
  private Boolean hasMultiUser;
  private Boolean hasAdvancedReports;
  private Boolean hasElectronicInvoice;
  private Boolean hasCreditNote;
  private Boolean hasDebitNote;
  private Boolean hasRetention;
  private Boolean hasGuide;
  private Boolean hasPrioritySupport;
  private Boolean hasCustomBranding;
  private Boolean hasAutomatedBackups;
}
