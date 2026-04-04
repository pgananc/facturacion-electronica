package com.wallparisoft.ebillbilling.response;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebillbilling.dto.DebitNoteDto;
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
public class DebitNoteResponse extends BasicResponse {

  List<DebitNoteDto> debitNoteDtos;
  DebitNoteDto debitNoteDto;
  Long idDebitNote;

  // Campos de autorización SRI
  String debitNoteNumber;
  String accessKey;
  String authorizationNumber;
  LocalDateTime authorizationDate;
  String receptionStatusSri;
  String authorizationStatusSri;
  String storagePath;
}
