package com.wallparisoft.integration.sri.model;

import com.wallparisoft.integration.sri.enums.FormaPagoEnum;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"formaPago", "total", "plazo", "unidadTiempo"})
public class Pago {

  @XmlElement private FormaPagoEnum formaPago;

  @XmlElement private String total;

  @XmlElement private String plazo;

  @XmlElement private String unidadTiempo;

  public FormaPagoEnum getFormaPago() {
    return formaPago;
  }

  public void setFormaPago(FormaPagoEnum formaPagoEnum) {
    this.formaPago = formaPagoEnum;
  }

  public String getTotal() {
    return total;
  }

  public void setTotal(String total) {
    this.total = total;
  }

  public String getPlazo() {
    return plazo;
  }

  public void setPlazo(String plazo) {
    this.plazo = plazo;
  }

  public String getUnidadTiempo() {
    return unidadTiempo;
  }

  public void setUnidadTiempo(String unidadTiempo) {
    this.unidadTiempo = unidadTiempo;
  }
}
