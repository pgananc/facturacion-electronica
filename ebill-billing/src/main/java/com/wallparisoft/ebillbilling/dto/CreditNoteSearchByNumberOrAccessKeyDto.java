package com.wallparisoft.ebillbilling.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * DTO para buscar notas de crédito autorizadas y activas por número de comprobante o clave de
 * acceso. Requiere idCompany obligatorio y al menos uno de los dos criterios de búsqueda. Los
 * nombres de los campos están homologados con las entidades CreditNote y AuthorizationSri.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditNoteSearchByNumberOrAccessKeyDto {

  /** ID de la compañía (obligatorio) */
  @NotNull(message = "idCompany es obligatorio")
  Long idCompany;

  /** Número de comprobante (opcional, pero se requiere creditNoteNumber o accessKey) */
  String creditNoteNumber;

  /** Clave de acceso (opcional, pero se requiere creditNoteNumber o accessKey) */
  String accessKey;
}
