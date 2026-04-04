package com.wallparisoft.ebillbilling.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.DebitNoteDetail;
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
 * DTO para la respuesta de lista de notas de débito. Incluye datos del cliente (buyerName,
 * buyerIdentification) para evitar consultas adicionales en el frontend.
 *
 * <p>NOTA: Las relaciones {@code authorizationSri} y {@code debitNoteDetails} no se incluyen en la
 * lista para evitar problemas de serialización JSON (anidamiento excesivo). Para obtener estos
 * datos completos, usar el endpoint individual GET /api/debit-notes/{id}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DebitNoteListResponseDto {

  Long idDebitNote;
  Long idCompany;
  Long idClient;
  Long idIssuancePoint;
  Long sequential;
  Integer retryCount;
  LocalDateTime issueDate;
  String debitNoteNumber;
  String environment;
  String additionalInformation;
  BigDecimal totalAmount;
  BigDecimal subtotalZero;
  BigDecimal subtotalIva;
  BigDecimal subtotalIce;
  BigDecimal discount;
  BigDecimal totalIva;
  BigDecimal totalIce;
  BigDecimal valorModificacion;
  BigDecimal valorTotal;
  String observation;
  Boolean emailSent;
  LocalDateTime emailSentDate;

  // Campos de la factura original
  Long originalInvoiceId;
  String originalAccessKey;
  String originalInvoiceNumber;
  LocalDateTime originalIssueDate;
  String documentTypeModified;

  // Relaciones - No incluidas en lista para evitar ciclos de serialización
  @JsonIgnore AuthorizationSri authorizationSri;
  @JsonIgnore List<DebitNoteDetail> debitNoteDetails;

  // Campos de BaseEntity
  LocalDateTime creationDate;
  LocalDateTime updateDate;
  Boolean status;

  // Datos del cliente (comprador)
  String buyerName;
  String buyerIdentification;

  // Estado calculado: BORRADOR, AUTORIZADO, DEVUELTA, EN_PROCESO
  String debitNoteStatus;
}
