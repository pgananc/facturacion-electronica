package com.wallparisoft.integration.sri.enums;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum ObligadoContabilidadEnum {
  /** Emision normal */
  @XmlEnumValue("SI")
  SI,

  /** Emision por Indisponibilidad del Sistema */
  @XmlEnumValue("NO")
  NO;
}
