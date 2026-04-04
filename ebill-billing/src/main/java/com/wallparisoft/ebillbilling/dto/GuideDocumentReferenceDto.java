package com.wallparisoft.ebillbilling.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
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
public class GuideDocumentReferenceDto implements Serializable {

  Long idGuideDocumentReference;

  Long idGuide;

  String documentType;

  String documentNumber;

  String accessKey;

  @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
  LocalDateTime issueDate;
}
