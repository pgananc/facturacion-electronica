package com.wallparisoft.authentication.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for bulk updating required plans for multiple features */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdatePlansDto {

  /**
   * Map where key is featureCode and value is list of required plan names Example: {
   * "INVOICE_CREATE": ["FREE", "BASIC"], "REPORT_ADVANCED": ["PROFESSIONAL", "ENTERPRISE"] }
   */
  private Map<String, List<String>> featurePlans;
}
