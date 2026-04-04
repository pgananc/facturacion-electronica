package com.wallparisoft.ebillbilling.dto.proforma;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wallparisoft.ebillbilling.dto.ClientDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
public class ProformaDto {

  Long idProforma;
  Long idCompany;
  Long idEstablishment;
  Long idIssuancePoint;
  Long idClient;
  String proformaNumber;

  @JsonFormat(pattern = "yyyy-MM-dd")
  LocalDate issueDate;

  @JsonFormat(pattern = "yyyy-MM-dd")
  LocalDate validUntil;

  String status;
  Long idInvoice;
  String additionalInformation;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime createdAt;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime updatedAt;

  String createdBy;

  ClientDto clientDto;
  List<ProformaDetailDto> details;

  // Totals (computed)
  BigDecimal subtotalZero;
  BigDecimal subtotalIva;
  BigDecimal subtotalIce;
  BigDecimal totalDiscount;
  BigDecimal totalAmount;
}
