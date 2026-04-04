package com.wallparisoft.ebillbilling.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** DTO para la búsqueda de notas de débito por compañía con filtros opcionales. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DebitNoteSearchByCompanyDto {

  /** ID de la compañía (obligatorio) */
  @NotNull(message = "idCompany es obligatorio")
  Long idCompany;

  /** Número de nota de débito (opcional) */
  String debitNoteNumber;

  /** Número de factura original (opcional) */
  String originalInvoiceNumber;
}
