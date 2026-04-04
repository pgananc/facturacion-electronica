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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDto {

  private Long idInvoice;

  private Long idCompany;

  private Long idClient;

  private Long idIssuancePoint;

  private Long sequential;

  @Nonnull private CompanyDto companyDto;
  @Nonnull private ClientDto clientDto;
  @Nonnull private ProductDto productDto;

  private LocalDateTime creationDate;

  /** Fecha de emisión de la factura */
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate issueDate;

  private String accessKey;

  private String invoiceNumber;

  /**
   * @deprecated Este campo ya no se utiliza para determinar el ambiente del SRI. El ambiente se
   *     configura en application.properties mediante la propiedad {@code sri.environment}.
   *     <p>El campo se mantiene por compatibilidad hacia atrás y se puede usar para almacenar el
   *     ambiente con el que se creó la factura (histórico), pero NO afecta el ambiente al que se
   *     envía al SRI.
   *     <p>Ver {@code com.wallparisoft.ebillbilling.config.SriConfiguration} para configurar el
   *     ambiente del SRI.
   * @see com.wallparisoft.ebillbilling.config.SriConfiguration
   */
  @Deprecated(since = "1.1.0")
  private String environment;

  private String authorizationNumber;

  private String additionalInformation;

  private LocalDateTime authorizationDate;

  private BigDecimal totalAmount;

  private BigDecimal discount;

  private BigDecimal tip;

  private BigDecimal subtotalZero;

  private BigDecimal subtotalIva;

  private BigDecimal subtotalIce;

  private BigDecimal subtotalDiscount;

  private String status;

  private List<InvoiceDetailDto> invoiceDetails;

  private EstablishmentDto establishment;

  private String password;

  // Campos adicionales para borradores
  private String buyerIdentification;
  private Integer buyerIdType;
  private String buyerName;
  private String buyerAddress;
  private String buyerPhone;
  private String buyerMail;

  // Alias para compatibilidad con frontend
  private List<InvoiceDetailDto> details; // Alias de invoiceDetails
  private List<PaymentDto> paymentMethods; // Array de formas de pago
}
