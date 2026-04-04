package com.wallparisoft.ebillbilling.dto;

import com.wallparisoft.integration.sri.model.GuiaRemision;
import java.time.LocalDateTime;

/**
 * Encapsula la información requerida para construir el RIDE (PDF) de una guía de remisión,
 * combinando los datos del XML de autorización del SRI con la estructura del documento.
 */
public record GuidePdfData(
    GuiaRemision guiaRemision,
    String estadoAutorizacion,
    String numeroAutorizacion,
    LocalDateTime fechaAutorizacion,
    String ambienteAutorizacion,
    String logoPath,
    String regime,
    String specialTaxpayerResolution) {

  public String getClaveAcceso() {
    // Extraer clave de acceso de InfoTributaria
    if (guiaRemision != null
        && guiaRemision.getInfoTributaria() != null
        && guiaRemision.getInfoTributaria().getClaveAcceso() != null) {
      return guiaRemision.getInfoTributaria().getClaveAcceso();
    }
    return "";
  }
}
