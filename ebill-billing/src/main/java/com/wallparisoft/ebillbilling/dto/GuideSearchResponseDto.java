package com.wallparisoft.ebillbilling.dto;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** DTO para la respuesta de búsqueda de guías de remisión por compañía. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuideSearchResponseDto {

  Long idGuide;

  String guideNumber;

  LocalDateTime createDate;

  /** Nombre del destinatario (cliente) */
  String destinatarioNombre;

  /** Identificación del transportista */
  String carrierIdentification;

  /** Nombre del transportista */
  String carrierName;

  /** Estado de autorización del SRI */
  String authorizationStatus;
}
