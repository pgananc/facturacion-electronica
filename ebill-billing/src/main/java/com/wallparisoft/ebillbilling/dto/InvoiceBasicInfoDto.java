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
 * DTO con información básica de una factura autorizada y activa. Contiene los campos esenciales
 * para consulta rápida. Los nombres de los campos están homologados con las entidades Invoice y
 * AuthorizationSri.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceBasicInfoDto {

  /** Clave de acceso de la factura autorizada (accessKey) */
  String accessKey;

  /** Número de autorización del SRI (authorizationNumber) */
  String authorizationNumber;

  /** Fecha de emisión de la factura (issueDate) */
  LocalDateTime issueDate;

  /** Número de comprobante (invoiceNumber) */
  String invoiceNumber;

  /** Lista de productos asociados al comprobante */
  List<ProductInfoDto> products;
}
