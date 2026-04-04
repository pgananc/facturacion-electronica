package com.wallparisoft.ebillbilling.dto;

import com.wallparisoft.integration.sri.model.NotaDebito;
import java.time.LocalDateTime;

/**
 * Encapsula la información requerida para construir el RIDE (PDF) de una nota de débito, combinando
 * los datos del XML de autorización del SRI con la estructura {@link NotaDebito}.
 */
public record DebitNotePdfData(
    NotaDebito notaDebito,
    String estadoAutorizacion,
    String numeroAutorizacion,
    LocalDateTime fechaAutorizacion,
    String ambienteAutorizacion,
    String logoPath,
    String regime,
    String specialTaxpayerResolution) {

  public String getClaveAcceso() {
    return notaDebito.getInfoTributaria().getClaveAcceso();
  }
}
