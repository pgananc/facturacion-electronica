package com.wallparisoft.authentication.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for bulk updating allowed roles for multiple features */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateRolesDto {

  /**
   * Map where key is featureCode and value is list of allowed role IDs Example: { "INVOICE_CREATE":
   * [1, 3], "CLIENT_CREATE": [1, 2, 3] }
   */
  private Map<String, List<Integer>> featureRoles;
}
