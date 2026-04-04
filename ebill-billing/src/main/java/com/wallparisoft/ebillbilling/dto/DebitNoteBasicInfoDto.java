package com.wallparisoft.ebillbilling.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * DTO con información básica de una nota de débito autorizada y activa. Contiene los campos
 * esenciales para consulta rápida. Los nombres de los campos están homologados con las entidades
 * DebitNote y AuthorizationSri.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DebitNoteBasicInfoDto {

  /** Clave de acceso de la nota de débito autorizada (accessKey) */
  String accessKey;

  /** Número de autorización del SRI (authorizationNumber) */
  String authorizationNumber;

  /** Fecha de emisión de la nota de débito (issueDate) */
  LocalDateTime issueDate;

  /** Número de comprobante (debitNoteNumber) */
  String debitNoteNumber;

  /** Lista de productos asociados al comprobante */
  List<ProductInfoDto> products;
}
