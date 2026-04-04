package com.wallparisoft.integration.sri.ws.recepcion;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Java class for respuestaSolicitud complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="respuestaSolicitud"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="estado" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="comprobantes" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="comprobante" type="{http://ec.gob.sri.ws.recepcion}comprobante" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "respuestaSolicitud",
    propOrder = {"estado", "comprobantes"})
public class RespuestaSolicitud {

  protected String estado;
  protected RespuestaSolicitud.Comprobantes comprobantes;

  /**
   * Gets the value of the estado property.
   *
   * @return possible object is {@link String }
   */
  public String getEstado() {
    return estado;
  }

  /**
   * Sets the value of the estado property.
   *
   * @param value allowed object is {@link String }
   */
  public void setEstado(String value) {
    this.estado = value;
  }

  /**
   * Gets the value of the comprobantes property.
   *
   * @return possible object is {@link RespuestaSolicitud.Comprobantes }
   */
  public RespuestaSolicitud.Comprobantes getComprobantes() {
    return comprobantes;
  }

  /**
   * Sets the value of the comprobantes property.
   *
   * @param value allowed object is {@link RespuestaSolicitud.Comprobantes }
   */
  public void setComprobantes(RespuestaSolicitud.Comprobantes value) {
    this.comprobantes = value;
  }

  /**
   * Java class for anonymous complex type.
   *
   * <p>The following schema fragment specifies the expected content contained within this class.
   *
   * <pre>
   * &lt;complexType&gt;
   *   &lt;complexContent&gt;
   *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
   *       &lt;sequence&gt;
   *         &lt;element name="comprobante" type="{http://ec.gob.sri.ws.recepcion}comprobante" maxOccurs="unbounded" minOccurs="0"/&gt;
   *       &lt;/sequence&gt;
   *     &lt;/restriction&gt;
   *   &lt;/complexContent&gt;
   * &lt;/complexType&gt;
   * </pre>
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(
      name = "",
      propOrder = {"comprobante"})
  public static class Comprobantes {

    protected List<Comprobante> comprobante;

    /**
     * Gets the value of the comprobante property.
     *
     * <p>This accessor method returns a reference to the live list, not a snapshot. Therefore any
     * modification you make to the returned list will be present inside the JAXB object. This is
     * why there is not a <CODE>set</CODE> method for the comprobante property.
     *
     * <p>For example, to add a new item, do as follows:
     *
     * <pre>
     *    getComprobante().add(newItem);
     * </pre>
     *
     * <p>Objects of the following type(s) are allowed in the list {@link Comprobante }
     */
    public List<Comprobante> getComprobante() {
      if (comprobante == null) {
        comprobante = new ArrayList<Comprobante>();
      }
      return this.comprobante;
    }
  }
}
