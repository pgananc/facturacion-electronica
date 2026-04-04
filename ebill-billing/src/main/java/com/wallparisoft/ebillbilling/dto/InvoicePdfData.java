package com.wallparisoft.ebillbilling.dto;

import com.wallparisoft.integration.sri.model.Factura;
import java.time.LocalDateTime;

/**
 * Encapsula la información requerida para construir el RIDE (PDF) de una factura, combinando los
 * datos del XML de autorización del SRI con la estructura {@link Factura}.
 */
public record InvoicePdfData(
    Factura factura,
    String estadoAutorizacion,
    String numeroAutorizacion,
    LocalDateTime fechaAutorizacion,
    String ambienteAutorizacion,
    String logoPath,
    String regime,
    String specialTaxpayerResolution) {

  public String getClaveAcceso() {
    return factura.getInfoTributaria().getClaveAcceso();
  }
}
