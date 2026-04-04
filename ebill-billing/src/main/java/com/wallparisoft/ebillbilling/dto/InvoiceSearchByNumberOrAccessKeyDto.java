package com.wallparisoft.ebillbilling.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * DTO para buscar facturas autorizadas y activas por número de factura o clave de acceso. Requiere
 * idCompany obligatorio y al menos uno de los dos criterios de búsqueda. Los nombres de los campos
 * están homologados con las entidades Invoice y AuthorizationSri.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceSearchByNumberOrAccessKeyDto {

  /** ID de la compañía (obligatorio) */
  @NotNull(message = "idCompany es obligatorio")
  Long idCompany;

  /** Número de factura (opcional, pero se requiere invoiceNumber o accessKey) */
  String invoiceNumber;

  /** Clave de acceso (opcional, pero se requiere invoiceNumber o accessKey) */
  String accessKey;
}
