package com.wallparisoft.integration.sri.model;

import com.wallparisoft.integration.sri.enums.MonedaEnum;
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
      "contribuyenteEspecial",
      "obligadoContabilidad",
      "rise",
      "codDocModificado",
      "numDocModificado",
      "fechaEmisionDocSustento",
      "totalSinImpuestos",
      "valorModificacion",
      "moneda",
      "totalConImpuestos",
      "motivo"
    })
@XmlSeeAlso({TotalImpuesto.class})
public class InfoNotaCredito implements Serializable {

  private static final long serialVersionUID = -5693500887762244834L;

  @XmlElement private String fechaEmision;

  @XmlElement private String dirEstablecimiento;

  @XmlElement private TipoIdentificacionCompradorEnum tipoIdentificacionComprador;

  @XmlElement private String razonSocialComprador;

  @XmlElement private String identificacionComprador;

  @XmlElement private String contribuyenteEspecial;

  @XmlElement private ObligadoContabilidadEnum obligadoContabilidad;

  @XmlElement private String rise;

  // Campos específicos de Nota de Crédito
  @XmlElement private String codDocModificado;

  @XmlElement private String numDocModificado;

  @XmlElement private String fechaEmisionDocSustento;

  @XmlElement private String totalSinImpuestos;

  @XmlElement private String valorModificacion;

  @XmlElement private MonedaEnum moneda;

  @XmlElementWrapper(name = "totalConImpuestos")
  @XmlElement(name = "totalImpuesto")
  private List<TotalImpuesto> totalConImpuestos;

  @XmlElement private String motivo;

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

  public String getContribuyenteEspecial() {
    return contribuyenteEspecial;
  }

  public void setContribuyenteEspecial(String contribuyenteEspecial) {
    this.contribuyenteEspecial = contribuyenteEspecial;
  }

  public ObligadoContabilidadEnum getObligadoContabilidad() {
    return obligadoContabilidad;
  }

  public void setObligadoContabilidad(ObligadoContabilidadEnum obligadoContabilidad) {
    this.obligadoContabilidad = obligadoContabilidad;
  }

  public String getRise() {
    return rise;
  }

  public void setRise(String rise) {
    this.rise = rise;
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

  public String getValorModificacion() {
    return valorModificacion;
  }

  public void setValorModificacion(String valorModificacion) {
    this.valorModificacion = valorModificacion;
  }

  public MonedaEnum getMoneda() {
    return moneda;
  }

  public void setMoneda(MonedaEnum moneda) {
    this.moneda = moneda;
  }

  public List<TotalImpuesto> getTotalConImpuestos() {
    return totalConImpuestos;
  }

  public void setTotalConImpuestos(List<TotalImpuesto> totalConImpuestos) {
    this.totalConImpuestos = totalConImpuestos;
  }

  public String getMotivo() {
    return motivo;
  }

  public void setMotivo(String motivo) {
    this.motivo = motivo;
  }
}
