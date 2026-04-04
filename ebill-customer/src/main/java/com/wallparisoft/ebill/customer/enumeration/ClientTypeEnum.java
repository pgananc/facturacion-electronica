package com.wallparisoft.ebill.customer.enumeration;

public enum ClientTypeEnum {
  CLIENT(1, "Cliente"),
  SUBJET_RET(2, "Sujeto Retenido"),
  ADDRESSEE(3, "Destinatario");

  ClientTypeEnum(final Integer code, final String description) {}
}
