package com.wallparisoft.integration.sri.ws.autorizacion;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Java class for autorizacionComprobante complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="autorizacionComprobante"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="claveAccesoComprobante" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "autorizacionComprobante",
    propOrder = {"claveAccesoComprobante"})
public class AutorizacionComprobante {

  protected String claveAccesoComprobante;

  /**
   * Gets the value of the claveAccesoComprobante property.
   *
   * @return possible object is {@link String }
   */
  public String getClaveAccesoComprobante() {
    return claveAccesoComprobante;
  }

  /**
   * Sets the value of the claveAccesoComprobante property.
   *
   * @param value allowed object is {@link String }
   */
  public void setClaveAccesoComprobante(String value) {
    this.claveAccesoComprobante = value;
  }
}
