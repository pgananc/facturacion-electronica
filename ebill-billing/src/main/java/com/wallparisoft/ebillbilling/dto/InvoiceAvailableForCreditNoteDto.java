package com.wallparisoft.ebillbilling.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * DTO para obtener los datos de una factura disponibles para crear nota de crédito. Incluye el
 * cálculo de cantidades y montos ya utilizados en NC previas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceAvailableForCreditNoteDto {

  Long idInvoice;
  String invoiceNumber;
  LocalDateTime issueDate;
  String accessKey;
  String authorizationNumber;

  // Datos del cliente
  ClientInfo client;

  // Totales
  BigDecimal totalInvoice;
  BigDecimal tip; // Propina - NO retornable mediante NC
  BigDecimal totalCreditNotesApplied;
  BigDecimal availableAmount; // = totalInvoice - tip - totalCreditNotesApplied

  // Detalles con cantidades disponibles
  List<InvoiceDetailAvailable> details;

  /** Información básica del cliente. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ClientInfo {
    Long idClient;
    String identification;
    String name;
    String email;
  }

  /** Detalle de factura con cantidades disponibles para NC. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InvoiceDetailAvailable {
    Long idInvoiceDetail;
    Long idProduct;
    String mainCode;
    String description;
    BigDecimal quantityInvoiced;
    BigDecimal quantityReturned;
    BigDecimal quantityAvailable;
    BigDecimal unitPrice;
    BigDecimal discount;
    BigDecimal subtotal;
    BigDecimal ivaPercentage;
  }
}
