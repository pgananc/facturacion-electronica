package com.wallparisoft.ebill.customer.enumeration;

import java.util.Set;

public enum IdentificationTypeEnum {
  RUC(0, "RUC", true),
  CED(1, "Cédula", true),
  PAS(2, "Pasaporte", false),
  CF(3, "Consumidor Final", true),
  IDE(4, "Identificación del exterior", false);

  private final int code;
  private final String description;
  private final boolean ecuadorianValidation;

  IdentificationTypeEnum(int code, String description, boolean ecuadorianValidation) {
    this.code = code;
    this.description = description;
    this.ecuadorianValidation = ecuadorianValidation;
  }

  public int getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  private static final Set<Integer> ECUADORIAN_TYPE_CODES;

  static {
    var codes = new java.util.HashSet<Integer>();
    for (IdentificationTypeEnum type : values()) {
      if (type.ecuadorianValidation) {
        codes.add(type.code);
      }
    }
    ECUADORIAN_TYPE_CODES = Set.copyOf(codes);
  }

  /** Determina si un código de tipo de identificación requiere validación ecuatoriana. */
  public static boolean requiresEcuadorianValidation(int code) {
    return ECUADORIAN_TYPE_CODES.contains(code);
  }

  public static IdentificationTypeEnum fromCode(int code) {
    for (IdentificationTypeEnum type : values()) {
      if (type.code == code) {
        return type;
      }
    }
    return null;
  }
}
