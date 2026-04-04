package com.wallparisoft.integration.sri.ws.autorizacion;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Java class for autorizacionComprobanteLote complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="autorizacionComprobanteLote"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="claveAccesoLote" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "autorizacionComprobanteLote",
    propOrder = {"claveAccesoLote"})
public class AutorizacionComprobanteLote {

  protected String claveAccesoLote;

  /**
   * Gets the value of the claveAccesoLote property.
   *
   * @return possible object is {@link String }
   */
  public String getClaveAccesoLote() {
    return claveAccesoLote;
  }

  /**
   * Sets the value of the claveAccesoLote property.
   *
   * @param value allowed object is {@link String }
   */
  public void setClaveAccesoLote(String value) {
    this.claveAccesoLote = value;
  }
}
