package com.wallparisoft.ebillbilling.dto.permission;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de respuesta para operaciones de actualización masiva. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateResponseDto {

  private Boolean success;
  private String message;
  private Integer totalUpdated;
  private Integer totalFailed;
  private List<UpdateResultDto> results;
  private LocalDateTime timestamp;
}
