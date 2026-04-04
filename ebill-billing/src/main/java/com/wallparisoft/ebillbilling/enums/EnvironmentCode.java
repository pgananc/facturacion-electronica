package com.wallparisoft.ebillbilling.enums;

/** Códigos de ambiente para el SRI Representa si la factura es para pruebas o producción */
public enum EnvironmentCode {

  /** Ambiente de pruebas Usado para facturación en ambiente de pruebas del SRI */
  PRUEBAS("1", "Ambiente de Pruebas"),

  /** Ambiente de producción Usado para facturación real en producción del SRI */
  PRODUCCION("2", "Ambiente de Producción");

  private final String code;
  private final String description;

  EnvironmentCode(String code, String description) {
    this.code = code;
    this.description = description;
  }

  /**
   * Obtiene el código del ambiente (1=Pruebas, 2=Producción)
   *
   * @return Código del ambiente según el SRI
   */
  public String getCode() {
    return code;
  }

  /**
   * Obtiene la descripción del ambiente
   *
   * @return Descripción legible
   */
  public String getDescription() {
    return description;
  }

  /**
   * Verifica si el ambiente es de pruebas
   *
   * @return true si es ambiente de pruebas
   */
  public boolean isPruebas() {
    return this == PRUEBAS;
  }

  /**
   * Verifica si el ambiente es de producción
   *
   * @return true si es ambiente de producción
   */
  public boolean isProduccion() {
    return this == PRODUCCION;
  }

  /**
   * Busca el enum por código
   *
   * @param code Código a buscar ("1" o "2")
   * @return El enum correspondiente o PRUEBAS por defecto
   */
  public static EnvironmentCode fromCode(String code) {
    if (code == null) {
      return PRUEBAS; // Default a pruebas por seguridad
    }

    for (EnvironmentCode env : values()) {
      if (env.code.equals(code)) {
        return env;
      }
    }

    return PRUEBAS; // Default a pruebas por seguridad
  }

  @Override
  public String toString() {
    return code + " - " + description;
  }
}
