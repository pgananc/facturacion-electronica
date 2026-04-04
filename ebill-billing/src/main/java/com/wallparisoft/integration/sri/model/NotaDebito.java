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

@XmlRootElement(name = "notaDebito")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"infoTributaria", "infoNotaDebito", "motivos", "infoAdicional"})
@XmlSeeAlso({
  InfoTributaria.class,
  InfoNotaDebito.class,
  DetalleNotaDebito.class,
  MotivoNotaDebito.class,
  CampoAdicional.class
})
public class NotaDebito implements Serializable {

  private static final long serialVersionUID = -4921604319752459194L;

  @XmlAttribute private String id;

  @XmlAttribute private String version;

  @XmlElement private InfoTributaria infoTributaria;

  @XmlElement private InfoNotaDebito infoNotaDebito;

  @XmlElementWrapper(name = "motivos")
  @XmlElement(name = "motivo")
  private List<MotivoNotaDebito> motivos;

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

  public InfoNotaDebito getInfoNotaDebito() {
    return infoNotaDebito;
  }

  public void setInfoNotaDebito(InfoNotaDebito infoNotaDebito) {
    this.infoNotaDebito = infoNotaDebito;
  }

  public List<MotivoNotaDebito> getMotivos() {
    return motivos;
  }

  public void setMotivos(List<MotivoNotaDebito> motivos) {
    this.motivos = motivos;
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
