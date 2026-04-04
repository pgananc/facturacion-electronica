package com.wallparisoft.integration.sri.model;

import com.wallparisoft.integration.sri.enums.AmbienteEnum;
import com.wallparisoft.integration.sri.enums.TipoComprobanteEnum;
import com.wallparisoft.integration.sri.enums.TipoEmisionEnum;
import com.wallparisoft.integration.sri.util.Modulo11Util;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    propOrder = {
      "ambiente",
      "tipoEmision",
      "razonSocial",
      "nombreComercial",
      "ruc",
      "claveAcceso",
      "codDoc",
      "estab",
      "ptoEmi",
      "secuencial",
      "dirMatriz"
    })
@XmlSeeAlso({InfoTributaria.class, InfoFactura.class, Detalle.class})
public class InfoTributaria implements Serializable {

  private static final long serialVersionUID = -2944593528360945418L;

  @XmlElement private AmbienteEnum ambiente;

  @XmlElement private TipoEmisionEnum tipoEmision;

  @XmlElement private String razonSocial;

  @XmlElement private String nombreComercial;

  @XmlElement private String ruc;

  @XmlElement private String claveAcceso;

  @XmlElement private String codDoc;

  @XmlElement private String estab;

  @XmlElement private String ptoEmi;

  @XmlElement private String secuencial;

  @XmlElement private String dirMatriz;

  public AmbienteEnum getAmbiente() {
    return ambiente;
  }

  public void setAmbiente(AmbienteEnum ambiente) {
    this.ambiente = ambiente;
  }

  public TipoEmisionEnum getTipoEmision() {
    return tipoEmision;
  }

  public void setTipoEmision(TipoEmisionEnum tipoEmision) {
    this.tipoEmision = tipoEmision;
  }

  public String getRazonSocial() {
    return razonSocial;
  }

  public void setRazonSocial(String razonSocial) {
    this.razonSocial = razonSocial;
  }

  public String getNombreComercial() {
    return nombreComercial;
  }

  public void setNombreComercial(String nombreComercial) {
    this.nombreComercial = nombreComercial;
  }

  public String getRuc() {
    return ruc;
  }

  public void setRuc(String ruc) {
    this.ruc = ruc;
  }

  public String getClaveAcceso() {
    return claveAcceso;
  }

  public void setClaveAcceso(String claveAcceso) {
    this.claveAcceso = claveAcceso;
  }

  public String generarClaveAcceso(
      Date fechaEmision, TipoComprobanteEnum tipoComprobante, String codigoNumerico) {
    SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
    if (codigoNumerico.length() != 8) {
      throw new IllegalArgumentException("Codigo numerico debe tener 8 digitos");
    }
    if (ruc == null) {
      throw new IllegalArgumentException("Numero de ruc no asignado");
    }
    if (ambiente == null) {
      throw new IllegalArgumentException("Ambiente no asignado");
    }
    if (estab == null) {
      throw new IllegalArgumentException("Establecimiento no asignado");
    }
    if (ptoEmi == null) {
      throw new IllegalArgumentException("Punto de Emision no asignado");
    }
    if (tipoEmision == null) {
      throw new IllegalArgumentException("Tipo de Emision no asignado");
    }
    if (secuencial == null) {
      throw new IllegalArgumentException("Secuencial no asignado");
    }
    String claveSinDigitoVerificador =
        sdf.format(fechaEmision)
            + tipoComprobante.getCodigo()
            + ruc
            + ambiente.getCodigo()
            + estab
            + ptoEmi
            + secuencial
            + codigoNumerico
            + tipoEmision.getCodigo();
    Modulo11Util modulo11Util = new Modulo11Util();

    String claveAcceso =
        claveSinDigitoVerificador
            + modulo11Util.obtenerDigitoVerificador(claveSinDigitoVerificador);
    if (claveAcceso.length() != 49) {
      throw new IllegalArgumentException(
          "Clave de acceso debe tener 49 digitos, actual: " + claveAcceso);
    }

    return claveAcceso;
  }

  public String getCodDoc() {
    return codDoc;
  }

  public void setCodDoc(String codDoc) {
    this.codDoc = codDoc;
  }

  public String getEstab() {
    return estab;
  }

  public void setEstab(String estab) {
    this.estab = estab;
  }

  public String getPtoEmi() {
    return ptoEmi;
  }

  public void setPtoEmi(String ptoEmi) {
    this.ptoEmi = ptoEmi;
  }

  public String getSecuencial() {
    return secuencial;
  }

  public void setSecuencial(String secuencial) {
    this.secuencial = secuencial;
  }

  public String getDirMatriz() {
    return dirMatriz;
  }

  public void setDirMatriz(String dirMatriz) {
    this.dirMatriz = dirMatriz;
  }
}
