package com.wallparisoft.integration.sri.model;

import com.wallparisoft.integration.sri.enums.ObligadoContabilidadEnum;
import com.wallparisoft.integration.sri.enums.TipoIdentificacionCompradorEnum;
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
      "fechaEmision",
      "dirEstablecimiento",
      "tipoIdentificacionComprador",
      "razonSocialComprador",
      "identificacionComprador",
      "obligadoContabilidad",
      "codDocModificado",
      "numDocModificado",
      "fechaEmisionDocSustento",
      "totalSinImpuestos",
      "impuestos",
      "valorTotal",
      "pagos"
    })
@XmlSeeAlso({ImpuestoNotaDebito.class, Pago.class})
public class InfoNotaDebito implements Serializable {

  private static final long serialVersionUID = -5693500887762244835L;

  @XmlElement private String fechaEmision;

  @XmlElement private String dirEstablecimiento;

  @XmlElement private TipoIdentificacionCompradorEnum tipoIdentificacionComprador;

  @XmlElement private String razonSocialComprador;

  @XmlElement private String identificacionComprador;

  @XmlElement private ObligadoContabilidadEnum obligadoContabilidad;

  // Campos específicos de Nota de Débito
  @XmlElement private String codDocModificado;

  @XmlElement private String numDocModificado;

  @XmlElement private String fechaEmisionDocSustento;

  @XmlElement private String totalSinImpuestos;

  @XmlElementWrapper(name = "impuestos")
  @XmlElement(name = "impuesto")
  private List<ImpuestoNotaDebito> impuestos;

  @XmlElement private String valorTotal;

  @XmlElementWrapper(name = "pagos")
  @XmlElement(name = "pago")
  private List<Pago> pagos;

  public String getFechaEmision() {
    return fechaEmision;
  }

  public void setFechaEmision(String fechaEmision) {
    this.fechaEmision = fechaEmision;
  }

  public String getDirEstablecimiento() {
    return dirEstablecimiento;
  }

  public void setDirEstablecimiento(String dirEstablecimiento) {
    this.dirEstablecimiento = dirEstablecimiento;
  }

  public TipoIdentificacionCompradorEnum getTipoIdentificacionComprador() {
    return tipoIdentificacionComprador;
  }

  public void setTipoIdentificacionComprador(
      TipoIdentificacionCompradorEnum tipoIdentificacionComprador) {
    this.tipoIdentificacionComprador = tipoIdentificacionComprador;
  }

  public String getRazonSocialComprador() {
    return razonSocialComprador;
  }

  public void setRazonSocialComprador(String razonSocialComprador) {
    this.razonSocialComprador = razonSocialComprador;
  }

  public String getIdentificacionComprador() {
    return identificacionComprador;
  }

  public void setIdentificacionComprador(String identificacionComprador) {
    this.identificacionComprador = identificacionComprador;
  }

  public ObligadoContabilidadEnum getObligadoContabilidad() {
    return obligadoContabilidad;
  }

  public void setObligadoContabilidad(ObligadoContabilidadEnum obligadoContabilidad) {
    this.obligadoContabilidad = obligadoContabilidad;
  }

  public String getCodDocModificado() {
    return codDocModificado;
  }

  public void setCodDocModificado(String codDocModificado) {
    this.codDocModificado = codDocModificado;
  }

  public String getNumDocModificado() {
    return numDocModificado;
  }

  public void setNumDocModificado(String numDocModificado) {
    this.numDocModificado = numDocModificado;
  }

  public String getFechaEmisionDocSustento() {
    return fechaEmisionDocSustento;
  }

  public void setFechaEmisionDocSustento(String fechaEmisionDocSustento) {
    this.fechaEmisionDocSustento = fechaEmisionDocSustento;
  }

  public String getTotalSinImpuestos() {
    return totalSinImpuestos;
  }

  public void setTotalSinImpuestos(String totalSinImpuestos) {
    this.totalSinImpuestos = totalSinImpuestos;
  }

  public List<ImpuestoNotaDebito> getImpuestos() {
    return impuestos;
  }

  public void setImpuestos(List<ImpuestoNotaDebito> impuestos) {
    this.impuestos = impuestos;
  }

  public String getValorTotal() {
    return valorTotal;
  }

  public void setValorTotal(String valorTotal) {
    this.valorTotal = valorTotal;
  }

  public List<Pago> getPagos() {
    return pagos;
  }

  public void setPagos(List<Pago> pagos) {
    this.pagos = pagos;
  }
}
