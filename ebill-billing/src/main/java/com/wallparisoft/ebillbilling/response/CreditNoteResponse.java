package com.wallparisoft.ebillbilling.response;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebillbilling.dto.CreditNoteDto;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = PRIVATE)
public class CreditNoteResponse extends BasicResponse {

  List<CreditNoteDto> creditNoteDtos;
  CreditNoteDto creditNoteDto;
  Long idCreditNote;

  // Campos de autorización SRI (similares a InvoiceResponse)
  String creditNoteNumber;
  String accessKey;
  String authorizationNumber;
  LocalDateTime authorizationDate;
  String receptionStatusSri;
  String authorizationStatusSri;
  String storagePath;
}
