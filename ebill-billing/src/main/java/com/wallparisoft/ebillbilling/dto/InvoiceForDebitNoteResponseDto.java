package com.wallparisoft.ebillbilling.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** DTO de respuesta para facturas disponibles para crear notas de débito. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceForDebitNoteResponseDto {

  Long idInvoice;
  String invoiceNumber;
  Long sequential;
  LocalDate issueDate;
  LocalDateTime creationDate;
  BigDecimal totalAmount;
  BigDecimal tip;
  BigDecimal availableAmount;
  Long idCompany;
  Long idIssuancePoint;
  String accessKey;

  /** Información de autorización del SRI */
  AuthorizationSriInfo authorizationSri;

  /** Datos del cliente */
  ClientDto clientDto;

  /** Nombre del comprador (para compatibilidad) */
  String buyerName;

  /** Identificación del comprador (para compatibilidad) */
  String buyerIdentification;

  /** Información de autorización del SRI */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AuthorizationSriInfo {
    Long idAuthorizationSri;
    String authorizationNumber;
    String accessKey;
    String authorizationStatusSri;
    LocalDateTime authorizationDate;
  }
}
