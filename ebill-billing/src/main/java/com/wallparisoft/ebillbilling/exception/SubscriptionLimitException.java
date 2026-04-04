package com.wallparisoft.ebillbilling.exception;

public class SubscriptionLimitException extends RuntimeException {

  private final String limitType;
  private final int currentCount;
  private final int limit;

  public SubscriptionLimitException(String message, String limitType, int currentCount, int limit) {
    super(message);
    this.limitType = limitType;
    this.currentCount = currentCount;
    this.limit = limit;
  }

  public String getLimitType() {
    return limitType;
  }

  public int getCurrentCount() {
    return currentCount;
  }

  public int getLimit() {
    return limit;
  }
}
