package com.wallparisoft.integration.sri.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"codigo", "codigoPorcentaje", "tarifa", "baseImponible", "valor"})
public class Impuesto implements Serializable {

  private static final long serialVersionUID = 6042092285712677811L;

  @XmlElement private String codigo;
  @XmlElement private String codigoPorcentaje;
  @XmlElement private String tarifa;
  @XmlElement private String baseImponible;
  @XmlElement private String valor;

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String impuesto) {
    this.codigo = impuesto;
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
}
