package com.wallparisoft.ebillbilling.response;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebillbilling.dto.GuideDto;
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
public class GuideResponse extends BasicResponse {

  List<GuideDto> guideDtos;
  GuideDto guideDto;
  Long idGuide;

  // Campos de autorización SRI
  String guideNumber;
  String accessKey;
  String authorizationNumber;
  LocalDateTime authorizationDate;
  String receptionStatusSri;
  String authorizationStatusSri;
  String storagePath;
}
