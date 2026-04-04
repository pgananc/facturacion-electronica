package com.wallparisoft.integration.sri.enums;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum ImpuestoRetencionEnum {
  /** Impuesto a la Renta */
  @XmlEnumValue("1")
  RENTA,
  /** Impuesto al Valor Agregado */
  @XmlEnumValue("2")
  IVA,
  /** Impuesto a la Salida de Divisas */
  @XmlEnumValue("6")
  ISD;
}
