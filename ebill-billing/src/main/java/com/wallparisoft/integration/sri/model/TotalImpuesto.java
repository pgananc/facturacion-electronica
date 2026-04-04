package com.wallparisoft.integration.sri.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    propOrder = {
      "codigo",
      "codigoPorcentaje",
      "descuentoAdicional",
      "baseImponible",
      "tarifa",
      "valor"
    })
public class TotalImpuesto implements Serializable {

  private static final long serialVersionUID = -138623113562583170L;

  @XmlElement private String codigo;

  @XmlElement private String codigoPorcentaje;

  @XmlElement private String baseImponible;

  @XmlElement(required = false)
  private String tarifa;

  @XmlElement private String valor;

  @XmlElement private String descuentoAdicional;

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigoImpuestoEnum) {
    this.codigo = codigoImpuestoEnum;
  }

  public String getCodigoPorcentaje() {
    return codigoPorcentaje;
  }

  public void setCodigoPorcentaje(String codigoPorcentaje) {
    this.codigoPorcentaje = codigoPorcentaje;
  }

  public String getTarifa() {
    return tarifa;
  }

  public void setTarifa(String tarifaEnum) {
    this.tarifa = tarifaEnum;
  }

  public String getBaseImponible() {
    return baseImponible;
  }

  public void setBaseImponible(String baseImponible) {
    this.baseImponible = baseImponible;
  }

  public String getValor() {
    return valor;
  }

  public void setValor(String valor) {
    this.valor = valor;
  }

  public void setDescuentoAdicional(String descuentoAdicional) {
    this.descuentoAdicional = descuentoAdicional;
  }
}
