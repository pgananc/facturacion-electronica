package com.wallparisoft.ebillbilling.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Respuesta estructurada para cuando se excede el rate limiting. Incluye información detallada
 * sobre los intentos y tiempo de espera.
 */
@Data
@Builder
public class RateLimitExceededResponse {

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime timestamp;

  private String message;
  private int attemptsUsed;
  private int maxAttempts;
  private long minutesUntilNextAttempt;
  private int cooldownMinutes;
}
