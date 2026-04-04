package com.wallparisoft.ebillbilling.dto.subscription;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO completo para planes de suscripción. Incluye todos los detalles del plan y sus
 * características.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanDto {

  // Campos básicos
  private Long idPlan;

  /** Código del plan (FREE, BASIC, PROFESSIONAL, ENTERPRISE) */
  private String code;

  /** Nombre del plan */
  private String name;

  /** Descripción del plan */
  private String description;

  /** Precio anual */
  private BigDecimal priceAnnual;

  /** Precio mensual */
  private BigDecimal priceMonthly;

  /** Límite de documentos anuales (-1 = ilimitado) */
  private Integer documentLimitYearly;

  /** Límite de usuarios */
  private Integer userLimit;

  /** Límite de establecimientos */
  private Integer establishmentLimit;

  /** Límite de puntos de emisión */
  private Integer issuancePointLimit;

  // Features booleanas
  private Boolean hasApiAccess;
  private Boolean hasMultiUser;
  private Boolean hasAdvancedReports;
  private Boolean hasElectronicInvoice;
  private Boolean hasCreditNote;
  private Boolean hasDebitNote;
  private Boolean hasRetention;
  private Boolean hasGuide;
  private Boolean hasPrioritySupport;
  private Boolean hasCustomBranding;
  private Boolean hasAutomatedBackups;

  private Boolean isActive;
  private Integer displayOrder;

  /** Lista de características del plan (para vista simplificada) */
  private List<String> features;

  // Aliases para retrocompatibilidad con endpoint /plans
  public BigDecimal getPrice() {
    return priceMonthly;
  }

  public Integer getDocumentLimit() {
    return documentLimitYearly;
  }
}
