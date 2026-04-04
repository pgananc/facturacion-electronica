package com.wallparisoft.ebillbilling.enums;

public enum ContactTypeEnum {
  ADD("Address"),
  PHO("PHONE"),
  EMA("EMAIL");

  private String description;

  private ContactTypeEnum(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
