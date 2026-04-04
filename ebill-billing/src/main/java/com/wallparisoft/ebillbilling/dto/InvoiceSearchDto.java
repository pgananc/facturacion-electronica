package com.wallparisoft.ebillbilling.dto;

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
public class InvoiceSearchDto {

  private String invoiceNumber;

  private Long idClient;

  private LocalDateTime creationDateFrom;

  private LocalDateTime creationDateTo;

  private String receptionStatusSri;

  private String authorizationStatusSri;
}
