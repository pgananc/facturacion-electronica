package com.wallparisoft.ebillbilling.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nonnull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * DTO para Guía de Remisión.
 *
 * <p><strong>Campos Obligatorios en Request:</strong>
 *
 * <ul>
 *   <li>{@code companyDto} - Datos de la empresa
 *   <li>{@code clientDto} - Datos del cliente destinatario
 *   <li>{@code carrierDto} - Datos del transportista (o idCarrier si existe)
 *   <li>{@code establishment} - Establecimiento y punto de emisión
 *   <li>{@code issueDate} - Fecha de emisión
 *   <li>{@code startDate} - Fecha inicio de traslado
 *   <li>{@code endDate} - Fecha fin de traslado
 *   <li>{@code originAddress} - Dirección de partida
 *   <li>{@code destinationAddress} - Dirección de destino
 *   <li>{@code guideDetails} - Lista de productos/items a transportar (mínimo 1)
 * </ul>
 *
 * <p><strong>Campos Generados Automáticamente (NO enviar en request):</strong>
 *
 * <ul>
 *   <li>{@code idGuide}, {@code sequential}, {@code accessKey}, {@code guideNumber}
 *   <li>{@code authorizationNumber}, {@code authorizationDate}, {@code creationDate}
 *   <li>{@code idCompany}, {@code idClient}, {@code idIssuancePoint} - Se extraen de objetos
 *       anidados
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuideDto {

  // ========== CAMPOS GENERADOS AUTOMÁTICAMENTE (NO enviar en request) ==========
  /** ID generado por la base de datos */
  private Long idGuide;

  /** Se extrae de companyDto.idCompany - NO enviar en request */
  private Long idCompany;

  /** Se extrae de clientDto.idClient - NO enviar en request */
  private Long idClient;

  /** Se extrae de establishment.idEstablishment - NO enviar en request */
  private Long idEstablishment;

  /** Se extrae de establishment.issuancePoint.idIssuancePoint - NO enviar en request */
  private Long idIssuancePoint;

  /** Generado automáticamente - NO enviar en request */
  private Long sequential;

  /** Generado automáticamente - NO enviar en request */
  private LocalDateTime creationDate;

  /** Generado automáticamente - NO enviar en request */
  private String accessKey;

  /** Generado automáticamente (estab-ptoEmi-secuencial) - NO enviar en request */
  private String guideNumber;

  /** Generado después de autorización - NO enviar en request */
  private String authorizationNumber;

  /** Generado después de autorización - NO enviar en request */
  private LocalDateTime authorizationDate;

  // ========== CAMPOS OBLIGATORIOS ==========
  @Nonnull private CompanyDto companyDto;

  @Nonnull private ClientDto clientDto;

  @Nonnull private CarrierDto carrierDto;

  /** Fecha de emisión de la guía */
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate issueDate;

  /** Fecha inicio de traslado */
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;

  /** Fecha fin de traslado */
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  /** Dirección de partida */
  @Nonnull private String originAddress;

  /** Dirección de destino */
  @Nonnull private String destinationAddress;

  /** Lista de productos/items a transportar (mínimo 1) */
  private List<GuideDetailDto> guideDetails;

  /** Establecimiento y punto de emisión */
  private EstablishmentDto establishment;

  // ========== CAMPOS OPCIONALES ==========
  /** Tipo de transporte (terrestre, aéreo, marítimo, etc.) */
  private String transportType;

  /** Placa del vehículo */
  private String transportPlate;

  /** Motivo del traslado */
  private String reasonForTransfer;

  /** Ruta a seguir (opcional) */
  private String route;

  /** Información adicional */
  private String additionalInformation;

  /** Lista de documentos relacionados (facturas, notas de crédito, etc.) */
  private List<GuideDocumentReferenceDto> documentReferences;

  /** ID del transportista (si ya existe en catálogo) */
  private Long idCarrier;

  /** Estado interno (BORRADOR, AUTORIZADO, etc.) - NO enviar en request */
  private String status;

  /**
   * Ambiente (se configura en backend desde application.properties: sri.environment).
   *
   * <p>Este campo se asigna automáticamente desde {@code SriConfiguration.getEnvironmentName()}
   * cuando se crea la guía. El valor puede ser "PRUEBAS" o "PRODUCCION".
   *
   * <p><strong>NO enviar en request</strong>
   */
  private String environment;

  /** Si se envió por email */
  private Boolean emailSent;

  /** Fecha de envío por email */
  private LocalDateTime emailSentDate;
}
