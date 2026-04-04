package com.wallparisoft.ebillbilling.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.entity.GuideDetail;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * DTO para la respuesta de lista de guías de remisión. Incluye datos del cliente (buyerName,
 * buyerIdentification) y transportista para evitar consultas adicionales en el frontend.
 *
 * <p>NOTA: Las relaciones {@code authorizationSri} y {@code guideDetails} no se incluyen en la
 * lista para evitar problemas de serialización JSON (anidamiento excesivo). Para obtener estos
 * datos completos, usar el endpoint individual GET /api/guide/{id}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuideListResponseDto {

  Long idGuide;
  Long idCompany;
  Long idClient;
  Long idEstablishment;
  Long idIssuancePoint;
  Long sequential;
  Integer retryCount;
  LocalDateTime issueDate;
  String guideNumber;
  String environment;
  String additionalInformation;
  Boolean emailSent;
  LocalDateTime emailSentDate;

  // Campos específicos de guía
  LocalDateTime startDate;
  LocalDateTime endDate;
  String transportType;
  String transportPlate;
  Long idCarrier;
  String originAddress;
  String destinationAddress;
  String reasonForTransfer;
  String route;

  // Relaciones - No incluidas en lista para evitar ciclos de serialización
  @JsonIgnore AuthorizationSri authorizationSri;
  @JsonIgnore List<GuideDetail> guideDetails;

  // Campos de BaseEntity
  LocalDateTime creationDate;
  LocalDateTime updateDate;
  Boolean status;

  // Datos del cliente (destinatario)
  String buyerName;
  String buyerIdentification;

  // Datos del transportista
  String carrierName;
  String carrierIdentification;

  // Estado calculado: BORRADOR, AUTORIZADO, DEVUELTA, EN_PROCESO
  String guideStatus;
}
