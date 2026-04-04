package com.wallparisoft.ebillbilling.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nonnull;
import java.math.BigDecimal;
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
 * DTO para Nota de Débito.
 *
 * <p><strong>Campos Obligatorios en Request:</strong>
 *
 * <ul>
 *   <li>{@code companyDto} - Datos de la empresa
 *   <li>{@code clientDto} - Datos del cliente
 *   <li>{@code establishment} - Establecimiento y punto de emisión
 *   <li>{@code issueDate} - Fecha de emisión
 *   <li>{@code originalInvoiceId} o {@code idInvoice} - ID de factura original
 *   <li>{@code originalInvoiceNumber} - Número de factura original (OBLIGATORIO para SRI)
 *   <li>{@code originalIssueDate} - Fecha de emisión de factura original (OBLIGATORIO para SRI)
 *   <li>{@code debitNoteDetails} - Lista de detalles/productos (mínimo 1)
 * </ul>
 *
 * <p><strong>Campos Opcionales (se calculan automáticamente si no se envían):</strong>
 *
 * <ul>
 *   <li>{@code totalAmount}, {@code valorModificacion}, {@code valorTotal}
 *   <li>{@code subtotalZero}, {@code subtotalIva}, {@code subtotalIce}
 *   <li>{@code totalIva}, {@code totalIce}
 *   <li>{@code originalAccessKey} - Se obtiene de la factura original si no se envía
 * </ul>
 *
 * <p><strong>Campos Generados Automáticamente (NO enviar en request):</strong>
 *
 * <ul>
 *   <li>{@code idDebitNote}, {@code sequential}, {@code accessKey}, {@code debitNoteNumber}
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
public class DebitNoteDto {

  // ========== CAMPOS GENERADOS AUTOMÁTICAMENTE (NO enviar en request) ==========
  /** ID generado por la base de datos */
  private Long idDebitNote;

  /** Se extrae de companyDto.idCompany - NO enviar en request */
  private Long idCompany;

  /** Se extrae de clientDto.idClient - NO enviar en request */
  private Long idClient;

  /** Se extrae de establishment.issuancePoint.idIssuancePoint - NO enviar en request */
  private Long idIssuancePoint;

  /** Generado automáticamente - NO enviar en request */
  private Long sequential;

  /** Generado automáticamente - NO enviar en request */
  private LocalDateTime creationDate;

  /** Generado automáticamente - NO enviar en request */
  private String accessKey;

  /** Generado automáticamente (estab-ptoEmi-secuencial) - NO enviar en request */
  private String debitNoteNumber;

  /** Generado después de autorización - NO enviar en request */
  private String authorizationNumber;

  /** Generado después de autorización - NO enviar en request */
  private LocalDateTime authorizationDate;

  // ========== CAMPOS OBLIGATORIOS ==========
  @Nonnull private CompanyDto companyDto;

  @Nonnull private ClientDto clientDto;

  /** Fecha de emisión de la nota de débito */
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate issueDate;

  /** Lista de detalles/productos (mínimo 1) - Fuente de verdad para motivos y totales */
  private List<InvoiceDetailDto> debitNoteDetails;

  /** Establecimiento y punto de emisión */
  private EstablishmentDto establishment;

  /** ID de la factura original que se modifica */
  private Long originalInvoiceId;

  /** Número de factura original (formato: estab-ptoEmi-secuencial) - OBLIGATORIO para SRI */
  private String originalInvoiceNumber;

  /** Fecha de emisión de la factura original - OBLIGATORIO para SRI */
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate originalIssueDate;

  // ========== CAMPOS OPCIONALES (se calculan automáticamente si no se envían) ==========
  /** Se calcula desde debitNoteDetails + impuestos */
  private BigDecimal totalAmount;

  /** Se calcula = suma de subtotal de todos los debitNoteDetails */
  private BigDecimal valorModificacion;

  /** Se calcula = valorModificacion + totalIva + totalIce */
  private BigDecimal valorTotal;

  /** Se calcula desde debitNoteDetails */
  private BigDecimal subtotalZero;

  /** Se calcula desde debitNoteDetails */
  private BigDecimal subtotalIva;

  /** Se calcula desde debitNoteDetails */
  private BigDecimal subtotalIce;

  /** Se calcula desde debitNoteDetails */
  private BigDecimal totalIva;

  /** Se calcula desde debitNoteDetails */
  private BigDecimal totalIce;

  /** Descuento total (opcional) */
  private BigDecimal discount;

  /** Clave de acceso de la factura original (se obtiene automáticamente si no se envía) */
  private String originalAccessKey;

  // ========== CAMPOS OPCIONALES ==========
  /** Información adicional (solo para BD, no se envía al SRI) */
  private String additionalInformation;

  /** Observación adicional (se envía como CampoAdicional en XML si existe) */
  private String observation;

  /** Tipo de documento modificado (01=Factura, 03=Liquidación, etc.) - Default: "01" */
  private String documentTypeModified;

  /** Lista de formas de pago (múltiples) - Si no se envía, se crea uno por defecto (01) */
  private List<PaymentDto> paymentMethods;

  /** Estado interno (BORRADOR, AUTORIZADO, etc.) - NO enviar en request */
  private String status;

  /**
   * Ambiente (se configura en backend desde application.properties: sri.environment).
   *
   * <p>Este campo se asigna automáticamente desde {@code SriConfiguration.getEnvironmentName()}
   * cuando se crea la nota de débito. El valor puede ser "PRUEBAS" o "PRODUCCION".
   *
   * <p><strong>NO enviar en request</strong> - Se obtiene de:
   *
   * <ul>
   *   <li>Configuración: {@code application.properties} → {@code sri.environment} (1=PRUEBAS,
   *       2=PRODUCCION)
   *   <li>Clase: {@code com.wallparisoft.ebillbilling.config.SriConfiguration}
   *   <li>Método: {@code sriConfiguration.getEnvironmentName()}
   * </ul>
   */
  private String environment;

  // ========== ALIAS PARA COMPATIBILIDAD (DEPRECADOS) ==========
  /**
   * @deprecated Alias de originalInvoiceId - Usar originalInvoiceId en su lugar
   */
  @Deprecated private Long idInvoice;

  /**
   * @deprecated Alias de debitNoteDetails - Usar debitNoteDetails en su lugar
   */
  @Deprecated private List<InvoiceDetailDto> details;
}
