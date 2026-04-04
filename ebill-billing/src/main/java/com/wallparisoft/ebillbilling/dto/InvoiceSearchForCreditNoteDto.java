package com.wallparisoft.ebillbilling.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** DTO para la búsqueda de facturas por compañía con filtros opcionales. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceSearchForCreditNoteDto {

  /** ID de la compañía (obligatorio) */
  @NotNull(message = "idCompany es obligatorio")
  Long idCompany;

  /** Número de autorización (opcional) */
  String numeroAutorizacion;

  /** Número de factura (opcional) */
  String numeroFactura;

  /** Nombre del cliente (opcional) */
  String nombreCliente;

  /** Identificación del cliente (opcional) */
  String identificacionCliente;
}
