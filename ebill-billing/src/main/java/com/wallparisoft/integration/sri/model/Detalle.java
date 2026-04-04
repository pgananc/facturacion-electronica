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
      "codigoPrincipal",
      "codigoAuxiliar",
      "descripcion",
      "cantidad",
      "precioUnitario",
      "descuento",
      "precioTotalSinImpuesto",
      "detallesAdicionales",
      "impuestos"
    })
@XmlSeeAlso({Impuesto.class})
public class Detalle implements Serializable {

  private static final long serialVersionUID = 1442890771223985798L;

  @XmlElement private String codigoPrincipal;

  @XmlElement private String codigoAuxiliar;

  @XmlElement private String descripcion;

  @XmlElement private String cantidad;

  @XmlElement private String precioUnitario;

  @XmlElement private String descuento;

  @XmlElement private String precioTotalSinImpuesto;

  @XmlElementWrapper(name = "impuestos")
  @XmlElement(name = "impuesto")
  private List<Impuesto> impuestos;

  @XmlElementWrapper(name = "detallesAdicionales")
  @XmlElement(name = "detAdicional")
  private List<DetAdicional> detallesAdicionales;

  public String getCodigoPrincipal() {
    return codigoPrincipal;
  }

  public void setCodigoPrincipal(String codigoPrincipal) {
    this.codigoPrincipal = codigoPrincipal;
  }

  public String getCodigoAuxiliar() {
    return codigoAuxiliar;
  }

  public void setCodigoAuxiliar(String codigoAuxiliar) {
    this.codigoAuxiliar = codigoAuxiliar;
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

  public List<Impuesto> getImpuestos() {
    return impuestos;
  }

  public void setImpuestos(List<Impuesto> impuestos) {
    this.impuestos = impuestos;
  }

  public List<DetAdicional> getDetallesAdicionales() {
    return detallesAdicionales;
  }

  public void setDetallesAdicionales(List<DetAdicional> detallesAdicionales) {
    this.detallesAdicionales = detallesAdicionales;
  }
}
