package com.wallparisoft.ebillbilling.dto.proforma;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
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
public class ProformaListResponseDto {

  Long idProforma;
  String proformaNumber;

  @JsonFormat(pattern = "yyyy-MM-dd")
  LocalDate issueDate;

  @JsonFormat(pattern = "yyyy-MM-dd")
  LocalDate validUntil;

  String status;
  Long idInvoice;
  BigDecimal totalAmount;

  Long idClient;
  String clientIdentification;
  String clientName;
}
