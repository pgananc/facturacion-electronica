package com.wallparisoft.ebillbilling.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** DTO para la búsqueda de notas de crédito por compañía con filtros opcionales. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditNoteSearchByCompanyDto {

  /** ID de la compañía (obligatorio) */
  @NotNull(message = "idCompany es obligatorio")
  Long idCompany;

  /** Número de nota de crédito (opcional) */
  String creditNoteNumber;

  /** Número de factura original (opcional) */
  String originalInvoiceNumber;
}
