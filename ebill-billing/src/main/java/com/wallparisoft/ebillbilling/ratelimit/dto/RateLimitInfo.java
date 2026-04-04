package com.wallparisoft.ebillbilling.ratelimit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Información sobre los límites de rate limiting actuales. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitInfo {

  /** Límite máximo permitido */
  private int limit;

  /** Requests/descargas restantes */
  private int remaining;

  /** Timestamp de cuando se resetea el contador (Unix timestamp) */
  private long resetTime;

  /** Tipo de límite que se excedió (TOKEN o IP) */
  private String limitType;

  /** Ventana de tiempo (MINUTE o HOUR) */
  private String timeWindow;
}
