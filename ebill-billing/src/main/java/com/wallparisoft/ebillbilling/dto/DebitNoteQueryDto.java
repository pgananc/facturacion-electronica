package com.wallparisoft.ebillbilling.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class DebitNoteQueryDto {

  private String debitNoteNumber;

  private String buyerIdentification;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate creationDateFrom;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate creationDateTo;

  private String receptionStatusSri;

  private String authorizationStatusSri;
}
