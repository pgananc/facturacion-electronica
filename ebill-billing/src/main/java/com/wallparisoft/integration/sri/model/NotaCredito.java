package com.wallparisoft.integration.sri.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.List;

@XmlRootElement(name = "notaCredito")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"infoTributaria", "infoNotaCredito", "detalles", "infoAdicional"})
@XmlSeeAlso({
  InfoTributaria.class,
  InfoNotaCredito.class,
  DetalleNotaCredito.class,
  CampoAdicional.class
})
public class NotaCredito implements Serializable {

  private static final long serialVersionUID = -4921604319752459193L;

  @XmlAttribute private String id;

  @XmlAttribute private String version;

  @XmlElement private InfoTributaria infoTributaria;

  @XmlElement private InfoNotaCredito infoNotaCredito;

  @XmlElementWrapper(name = "detalles")
  @XmlElement(name = "detalle")
  private List<DetalleNotaCredito> detalles;

  @XmlElementWrapper(name = "infoAdicional")
  @XmlElement(name = "campoAdicional")
  private List<CampoAdicional> infoAdicional;

  public List<CampoAdicional> getInfoAdicional() {
    return infoAdicional;
  }

  public void setInfoAdicional(List<CampoAdicional> infoAdicional) {
    this.infoAdicional = infoAdicional;
  }

  public InfoTributaria getInfoTributaria() {
    return infoTributaria;
  }

  public void setInfoTributaria(InfoTributaria infoTributaria) {
    this.infoTributaria = infoTributaria;
  }

  public InfoNotaCredito getInfoNotaCredito() {
    return infoNotaCredito;
  }

  public void setInfoNotaCredito(InfoNotaCredito infoNotaCredito) {
    this.infoNotaCredito = infoNotaCredito;
  }

  public List<DetalleNotaCredito> getDetalles() {
    return detalles;
  }

  public void setDetalles(List<DetalleNotaCredito> detalles) {
    this.detalles = detalles;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
