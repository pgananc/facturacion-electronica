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
      "precioUnitario",
      "descuento",
      "precioTotalSinImpuesto",
      "detallesAdicionales",
      "impuestos"
    })
@XmlSeeAlso({ImpuestoNotaDebito.class})
public class DetalleNotaDebito implements Serializable {

  private static final long serialVersionUID = 1442890771223985800L;

  @XmlElement(name = "codigoInterno")
  private String codigoInterno;

  @XmlElement(name = "codigoAdicional", required = false)
  private String codigoAdicional;

  @XmlElement private String descripcion;

  @XmlElement private String cantidad;

  @XmlElement private String precioUnitario;

  @XmlElement private String descuento;

  @XmlElement private String precioTotalSinImpuesto;

  @XmlElementWrapper(name = "impuestos")
  @XmlElement(name = "impuesto")
  private List<ImpuestoNotaDebito> impuestos;

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
    if (codigoAdicional == null
        || codigoAdicional.trim().isEmpty()
        || "0".equals(codigoAdicional.trim())) {
      return null;
    }
    return codigoAdicional;
  }

  public void setCodigoAdicional(String codigoAdicional) {
    if (codigoAdicional != null
        && !codigoAdicional.trim().isEmpty()
        && !"0".equals(codigoAdicional.trim())) {
      this.codigoAdicional = codigoAdicional;
    } else {
      this.codigoAdicional = null;
    }
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

  public String getPrecioUnitario() {
    return precioUnitario;
  }

  public void setPrecioUnitario(String precioUnitario) {
    this.precioUnitario = precioUnitario;
  }

  public String getDescuento() {
    return descuento;
  }

  public void setDescuento(String descuento) {
    this.descuento = descuento;
  }

  public String getPrecioTotalSinImpuesto() {
    return precioTotalSinImpuesto;
  }

  public void setPrecioTotalSinImpuesto(String precioTotalSinImpuesto) {
    this.precioTotalSinImpuesto = precioTotalSinImpuesto;
  }

  public List<ImpuestoNotaDebito> getImpuestos() {
    return impuestos;
  }

  public void setImpuestos(List<ImpuestoNotaDebito> impuestos) {
    this.impuestos = impuestos;
  }

  public List<DetAdicional> getDetallesAdicionales() {
    return detallesAdicionales;
  }

  public void setDetallesAdicionales(List<DetAdicional> detallesAdicionales) {
    this.detallesAdicionales = detallesAdicionales;
  }
}
