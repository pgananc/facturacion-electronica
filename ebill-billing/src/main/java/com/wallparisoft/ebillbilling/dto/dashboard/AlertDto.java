package com.wallparisoft.ebillbilling.dto.dashboard;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {
  private String id;
  private AlertType type;
  private AlertPriority priority;
  private String title;
  private String message;
  private Integer count;
  private String actionUrl;
  private Map<String, Object> metadata;
  private LocalDateTime createdAt;
}
