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

@XmlRootElement(name = "guiaRemision")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"infoTributaria", "infoGuiaRemision", "destinatarios", "infoAdicional"})
@XmlSeeAlso({
  InfoTributaria.class,
  InfoGuiaRemision.class,
  Destinatario.class,
  DetalleGuiaRemision.class,
  CampoAdicional.class
})
public class GuiaRemision implements Serializable {

  private static final long serialVersionUID = -4921604319752459195L;

  @XmlAttribute private String id;

  @XmlAttribute private String version;

  @XmlElement private InfoTributaria infoTributaria;

  @XmlElement private InfoGuiaRemision infoGuiaRemision;

  @XmlElementWrapper(name = "destinatarios")
  @XmlElement(name = "destinatario")
  private List<Destinatario> destinatarios;

  @XmlElementWrapper(name = "infoAdicional")
  @XmlElement(name = "campoAdicional")
  private List<CampoAdicional> infoAdicional;

  public InfoTributaria getInfoTributaria() {
    return infoTributaria;
  }

  public void setInfoTributaria(InfoTributaria infoTributaria) {
    this.infoTributaria = infoTributaria;
  }

  public InfoGuiaRemision getInfoGuiaRemision() {
    return infoGuiaRemision;
  }

  public void setInfoGuiaRemision(InfoGuiaRemision infoGuiaRemision) {
    this.infoGuiaRemision = infoGuiaRemision;
  }

  public List<Destinatario> getDestinatarios() {
    return destinatarios;
  }

  public void setDestinatarios(List<Destinatario> destinatarios) {
    this.destinatarios = destinatarios;
  }

  public List<CampoAdicional> getInfoAdicional() {
    return infoAdicional;
  }

  public void setInfoAdicional(List<CampoAdicional> infoAdicional) {
    this.infoAdicional = infoAdicional;
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
