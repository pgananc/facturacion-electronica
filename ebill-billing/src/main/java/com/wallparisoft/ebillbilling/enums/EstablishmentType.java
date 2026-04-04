package com.wallparisoft.ebillbilling.enums;

/** Tipos de establecimiento según la normativa del SRI */
public enum EstablishmentType {

  /**
   * Establecimiento principal de la empresa La dirección matriz es la misma dirección del
   * establecimiento
   */
  MATRIZ("MATRIZ", "Establecimiento Matriz"),

  /** Establecimiento secundario o sucursal La dirección matriz es la del establecimiento padre */
  SUCURSAL("SUCURSAL", "Sucursal");

  private final String code;
  private final String description;

  EstablishmentType(String code, String description) {
    this.code = code;
    this.description = description;
  }

  /**
   * Obtiene el código del tipo de establecimiento
   *
   * @return Código (MATRIZ o SUCURSAL)
   */
  public String getCode() {
    return code;
  }

  /**
   * Obtiene la descripción del tipo de establecimiento
   *
   * @return Descripción legible
   */
  public String getDescription() {
    return description;
  }

  /**
   * Busca el enum por código (case-insensitive)
   *
   * @param code Código a buscar (MATRIZ o SUCURSAL)
   * @return El enum correspondiente o null si no existe
   */
  public static EstablishmentType fromCode(String code) {
    if (code == null) {
      return null;
    }

    for (EstablishmentType type : values()) {
      if (type.code.equalsIgnoreCase(code)) {
        return type;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return code;
  }
}
