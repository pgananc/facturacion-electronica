package com.wallparisoft.integration.sri.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    propOrder = {
      "codigoInterno",
      "codigoAdicional",
      "descripcion",
      "cantidad",
      "detallesAdicionales"
    })
@XmlSeeAlso({})
public class DetalleGuiaRemision implements Serializable {

  private static final long serialVersionUID = 1442890771223985799L;

  @XmlElement private String codigoInterno;

  @XmlElement private String codigoAdicional;

  @XmlElement private String descripcion;

  @XmlElement private String cantidad;

  @XmlElementWrapper(name = "detallesAdicionales")
  @XmlElement(name = "detAdicional")
  private List<DetAdicional> detallesAdicionales;

  public String getCodigoInterno() {
    return codigoInterno;
  }

  public void setCodigoInterno(String codigoInterno) {
    this.codigoInterno = codigoInterno;
  }

  public String getCodigoAdicional() {
    return codigoAdicional;
  }

  public void setCodigoAdicional(String codigoAdicional) {
    this.codigoAdicional = codigoAdicional;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public String getCantidad() {
    return cantidad;
  }

  public void setCantidad(String cantidad) {
    this.cantidad = cantidad;
  }

  public List<DetAdicional> getDetallesAdicionales() {
    return detallesAdicionales;
  }

  public void setDetallesAdicionales(List<DetAdicional> detallesAdicionales) {
    this.detallesAdicionales = detallesAdicionales;
  }
}
