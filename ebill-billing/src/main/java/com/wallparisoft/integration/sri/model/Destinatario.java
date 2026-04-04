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
      "identificacionDestinatario",
      "razonSocialDestinatario",
      "dirDestinatario",
      "motivoTraslado",
      "docAduaneroUnico",
      "codEstabDestino",
      "ruta",
      "codDocSustento",
      "numDocSustento",
      "numAutDocSustento",
      "fechaEmisionDocSustento",
      "detalles"
    })
@XmlSeeAlso({DetalleGuiaRemision.class})
public class Destinatario implements Serializable {

  private static final long serialVersionUID = -5693500887762244834L;

  @XmlElement private String identificacionDestinatario;

  @XmlElement private String razonSocialDestinatario;

  @XmlElement private String dirDestinatario;

  @XmlElement private String motivoTraslado;

  @XmlElement private String docAduaneroUnico;

  @XmlElement private String codEstabDestino;

  @XmlElement private String ruta;

  @XmlElement private String codDocSustento;

  @XmlElement private String numDocSustento;

  @XmlElement private String numAutDocSustento;

  @XmlElement private String fechaEmisionDocSustento;

  @XmlElementWrapper(name = "detalles")
  @XmlElement(name = "detalle")
  private List<DetalleGuiaRemision> detalles;

  public String getIdentificacionDestinatario() {
    return identificacionDestinatario;
  }

  public void setIdentificacionDestinatario(String identificacionDestinatario) {
    this.identificacionDestinatario = identificacionDestinatario;
  }

  public String getRazonSocialDestinatario() {
    return razonSocialDestinatario;
  }

  public void setRazonSocialDestinatario(String razonSocialDestinatario) {
    this.razonSocialDestinatario = razonSocialDestinatario;
  }

  public String getDirDestinatario() {
    return dirDestinatario;
  }

  public void setDirDestinatario(String dirDestinatario) {
    this.dirDestinatario = dirDestinatario;
  }

  public String getMotivoTraslado() {
    return motivoTraslado;
  }

  public void setMotivoTraslado(String motivoTraslado) {
    this.motivoTraslado = motivoTraslado;
  }

  public String getDocAduaneroUnico() {
    return docAduaneroUnico;
  }

  public void setDocAduaneroUnico(String docAduaneroUnico) {
    this.docAduaneroUnico = docAduaneroUnico;
  }

  public String getCodEstabDestino() {
    return codEstabDestino;
  }

  public void setCodEstabDestino(String codEstabDestino) {
    this.codEstabDestino = codEstabDestino;
  }

  public String getRuta() {
    return ruta;
  }

  public void setRuta(String ruta) {
    this.ruta = ruta;
  }

  public String getCodDocSustento() {
    return codDocSustento;
  }

  public void setCodDocSustento(String codDocSustento) {
    this.codDocSustento = codDocSustento;
  }

  public String getNumDocSustento() {
    return numDocSustento;
  }

  public void setNumDocSustento(String numDocSustento) {
    this.numDocSustento = numDocSustento;
  }

  public String getNumAutDocSustento() {
    return numAutDocSustento;
  }

  public void setNumAutDocSustento(String numAutDocSustento) {
    this.numAutDocSustento = numAutDocSustento;
  }

  public String getFechaEmisionDocSustento() {
    return fechaEmisionDocSustento;
  }

  public void setFechaEmisionDocSustento(String fechaEmisionDocSustento) {
    this.fechaEmisionDocSustento = fechaEmisionDocSustento;
  }

  public List<DetalleGuiaRemision> getDetalles() {
    return detalles;
  }

  public void setDetalles(List<DetalleGuiaRemision> detalles) {
    this.detalles = detalles;
  }
}
