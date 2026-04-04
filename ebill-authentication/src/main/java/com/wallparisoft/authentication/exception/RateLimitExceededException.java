package com.wallparisoft.authentication.exception;

import java.time.LocalDateTime;

public class RateLimitExceededException extends RuntimeException {

  private final LocalDateTime timestamp;
  private final long minutesUntilNextAttempt;
  private final int attemptsUsed;
  private final int maxAttempts;
  private final int cooldownMinutes;

  public RateLimitExceededException(
      String message,
      long minutesUntilNextAttempt,
      int attemptsUsed,
      int maxAttempts,
      int cooldownMinutes) {
    super(message);
    this.timestamp = LocalDateTime.now();
    this.minutesUntilNextAttempt = minutesUntilNextAttempt;
    this.attemptsUsed = attemptsUsed;
    this.maxAttempts = maxAttempts;
    this.cooldownMinutes = cooldownMinutes;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public long getMinutesUntilNextAttempt() {
    return minutesUntilNextAttempt;
  }

  public int getAttemptsUsed() {
    return attemptsUsed;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public int getCooldownMinutes() {
    return cooldownMinutes;
  }

  public long getSecondsUntilNextAttempt() {
    return minutesUntilNextAttempt * 60;
  }
}
