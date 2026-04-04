package com.wallparisoft.ebillbilling.dto;

import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.CreditNoteDetail;
import com.wallparisoft.ebillbilling.entity.CreditNoteReason;
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
 * DTO para la respuesta de lista de notas de crédito. Incluye datos del cliente (buyerName,
 * buyerIdentification) para evitar consultas adicionales en el frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditNoteListResponseDto {

  Long idCreditNote;
  Long idCompany;
  Long idClient;
  Long idIssuancePoint;
  Long sequential;
  Integer retryCount;
  LocalDateTime issueDate;
  String creditNoteNumber;
  String environment;
  String additionalInformation;
  BigDecimal totalAmount;
  BigDecimal subtotalZero;
  BigDecimal subtotalIva;
  BigDecimal subtotalIce;
  BigDecimal discount;
  BigDecimal totalIva;
  BigDecimal totalIce;
  BigDecimal tip;
  BigDecimal valorModificacion;
  String observation;
  Boolean emailSent;
  LocalDateTime emailSentDate;

  // Campos de la factura original
  Long originalInvoiceId;
  String originalAccessKey;
  String originalInvoiceNumber;
  LocalDateTime originalIssueDate;
  String documentTypeModified;
  String reason;

  // Relaciones
  CreditNoteReason creditNoteReason;
  AuthorizationSri authorizationSri;
  List<CreditNoteDetail> creditNoteDetails;

  // Campos de BaseEntity
  LocalDateTime creationDate;
  LocalDateTime updateDate;
  Boolean status;

  // Datos del cliente (comprador) - NUEVOS CAMPOS
  String buyerName;
  String buyerIdentification;

  // Estado calculado: BORRADOR, AUTORIZADO, DEVUELTA, EN_PROCESO
  String creditNoteStatus;
}
