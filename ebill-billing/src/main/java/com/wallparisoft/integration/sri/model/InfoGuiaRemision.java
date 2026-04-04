package com.wallparisoft.integration.sri.model;

import com.wallparisoft.integration.sri.enums.ObligadoContabilidadEnum;
import com.wallparisoft.integration.sri.enums.TipoIdentificacionCompradorEnum;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    propOrder = {
      "dirEstablecimiento",
      "dirPartida",
      "razonSocialTransportista",
      "tipoIdentificacionTransportista",
      "rucTransportista",
      "rise",
      "obligadoContabilidad",
      "contribuyenteEspecial",
      "fechaIniTransporte",
      "fechaFinTransporte",
      "placa"
    })
@XmlSeeAlso({})
public class InfoGuiaRemision implements Serializable {

  private static final long serialVersionUID = -5693500887762244835L;

  @XmlElement private String dirEstablecimiento;

  @XmlElement private String dirPartida;

  @XmlElement private String razonSocialTransportista;

  @XmlElement private TipoIdentificacionCompradorEnum tipoIdentificacionTransportista;

  @XmlElement private String rucTransportista;

  @XmlElement private String rise;

  @XmlElement private ObligadoContabilidadEnum obligadoContabilidad;

  @XmlElement private String contribuyenteEspecial;

  @XmlElement private String placa;

  @XmlElement private String fechaIniTransporte;

  @XmlElement private String fechaFinTransporte;

  public String getDirEstablecimiento() {
    return dirEstablecimiento;
  }

  public void setDirEstablecimiento(String dirEstablecimiento) {
    this.dirEstablecimiento = dirEstablecimiento;
  }

  public String getDirPartida() {
    return dirPartida;
  }

  public void setDirPartida(String dirPartida) {
    this.dirPartida = dirPartida;
  }

  public String getRazonSocialTransportista() {
    return razonSocialTransportista;
  }

  public void setRazonSocialTransportista(String razonSocialTransportista) {
    this.razonSocialTransportista = razonSocialTransportista;
  }

  public TipoIdentificacionCompradorEnum getTipoIdentificacionTransportista() {
    return tipoIdentificacionTransportista;
  }

  public void setTipoIdentificacionTransportista(
      TipoIdentificacionCompradorEnum tipoIdentificacionTransportista) {
    this.tipoIdentificacionTransportista = tipoIdentificacionTransportista;
  }

  public String getRucTransportista() {
    return rucTransportista;
  }

  public void setRucTransportista(String rucTransportista) {
    this.rucTransportista = rucTransportista;
  }

  public String getRise() {
    return rise;
  }

  public void setRise(String rise) {
    this.rise = rise;
  }

  public ObligadoContabilidadEnum getObligadoContabilidad() {
    return obligadoContabilidad;
  }

  public void setObligadoContabilidad(ObligadoContabilidadEnum obligadoContabilidad) {
    this.obligadoContabilidad = obligadoContabilidad;
  }

  public String getContribuyenteEspecial() {
    return contribuyenteEspecial;
  }

  public void setContribuyenteEspecial(String contribuyenteEspecial) {
    this.contribuyenteEspecial = contribuyenteEspecial;
  }

  public String getPlaca() {
    return placa;
  }

  public void setPlaca(String placa) {
    this.placa = placa;
  }

  public String getFechaIniTransporte() {
    return fechaIniTransporte;
  }

  public void setFechaIniTransporte(String fechaIniTransporte) {
    this.fechaIniTransporte = fechaIniTransporte;
  }

  public String getFechaFinTransporte() {
    return fechaFinTransporte;
  }

  public void setFechaFinTransporte(String fechaFinTransporte) {
    this.fechaFinTransporte = fechaFinTransporte;
  }
}
