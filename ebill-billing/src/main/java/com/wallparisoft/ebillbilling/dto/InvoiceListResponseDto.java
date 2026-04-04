package com.wallparisoft.ebillbilling.dto;

import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.InvoiceDetail;
import com.wallparisoft.ebillbilling.entity.InvoicePayment;
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
 * DTO para la respuesta de listado de facturas. Incluye todos los campos de Invoice más los datos
 * básicos del cliente (name e identification).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceListResponseDto {

  Long idInvoice;
  Long idCompany;
  Long idClient;
  Long idIssuancePoint;
  Long sequential;
  Integer retryCount;
  LocalDateTime issueDate;
  String invoiceNumber;
  String environment;
  String additionalInformation;
  BigDecimal totalAmount;
  BigDecimal tip;
  BigDecimal subtotalZero;
  BigDecimal subtotalIva;
  BigDecimal subtotalIce;
  BigDecimal discount;
  Boolean emailSent;
  LocalDateTime emailSentDate;
  AuthorizationSri authorizationSri;
  List<InvoiceDetail> invoiceDetails;
  List<InvoicePayment> invoicePayments;

  // Campos de BaseEntity
  LocalDateTime creationDate;
  LocalDateTime updateDate;
  Boolean status;

  // Datos del cliente (nuevos campos)
  String clientName;
  String clientIdentification;

  // Monto disponible para notas de crédito (totalAmount - suma de NC autorizadas)
  BigDecimal availableAmount;
}
