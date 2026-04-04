package com.wallparisoft.integration.sri.enums;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum TipoEmisionEnum {
  /** Emision normal */
  @XmlEnumValue("1")
  NORMAL("1"),

  /** Emision por Indisponibilidad del Sistema */
  @XmlEnumValue("2")
  INDISPONIBILIDAD("2");

  private String codigo;

  private TipoEmisionEnum(String codigo) {
    this.codigo = codigo;
  }

  public String getCodigo() {
    return codigo;
  }
}
