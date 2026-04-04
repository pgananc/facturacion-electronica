package com.wallparisoft.ebillbilling.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class InvoiceDraftResponseDto {

  Long idInvoice;

  Long idCompany;

  Long idClient;

  LocalDateTime issueDate;

  LocalDateTime creationDate;

  String invoiceNumber;

  String environment;

  String additionalInformation;

  BigDecimal totalAmount;

  BigDecimal tip;

  BigDecimal subtotalZero;

  BigDecimal subtotalIva;

  BigDecimal subtotalIce;

  BigDecimal discount;

  Integer retryCount;

  String paymentMethodName;

  Boolean status;

  // Campos del cliente para la tabla de borradores
  String buyerIdentification;
  Integer buyerIdType;
  String buyerName;
  String buyerAddress;
  String buyerPhone;
  String buyerMail;

  // ClientDto completo (OPCIONAL pero recomendado)
  ClientDto clientDto;
}
