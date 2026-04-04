package com.wallparisoft.ebillbilling.dto;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentQueryDto {

  /**
   * Número de documento (factura o nota de crédito) - búsqueda parcial Ejemplo: "001-001-000000123"
   * o "001"
   */
  private String documentNumber;

  /**
   * Identificación del comprador (cédula/RUC) Ejemplo: "1234567890" Nota: Se busca primero el
   * cliente por identificación y luego sus documentos
   */
  private String buyerIdentification;

  /** Fecha de creación desde (inclusive) Ejemplo: "2025-11-01" */
  private LocalDate creationDateFrom;

  /** Fecha de creación hasta (inclusive) Ejemplo: "2025-11-30" */
  private LocalDate creationDateTo;

  /**
   * Tipo de comprobante a consultar: 0 = Facturas y Notas de Crédito (ambos) 1 = Solo Facturas 2 =
   * Solo Notas de Crédito
   */
  private Integer documentType;

  private String receptionStatusSri;

  private String authorizationStatusSri;
}
