package com.wallparisoft.ebillbilling.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** DTO para la búsqueda de facturas por compañía y rango de fechas con filtros opcionales. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceSearchByCompanyAndDateDto {

  /** ID de la compañía (obligatorio) */
  @NotNull(message = "idCompany es obligatorio")
  Long idCompany;

  /** Fecha de creación desde (inclusive) - Opcional */
  LocalDate creationDateFrom;

  /** Fecha de creación hasta (inclusive) - Opcional */
  LocalDate creationDateTo;

  /** Número de autorización (opcional) */
  String numeroAutorizacion;

  /** Número de factura (opcional) */
  String numeroFactura;

  /** Nombre del cliente (opcional) */
  String nombreCliente;

  /** Identificación del cliente (opcional) */
  String identificacionCliente;

  /** Estado de recepción del SRI (opcional) */
  String receptionStatusSri;

  /** Estado de autorización del SRI (opcional) */
  String authorizationStatusSri;
}
