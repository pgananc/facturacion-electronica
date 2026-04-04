package com.wallparisoft.ebillbilling.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "subscription_plan")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
public class SubscriptionPlan extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "subscription_plan_seq",
      sequenceName = "subscription_plan_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscription_plan_seq")
  @Column(name = "id_plan")
  Long idPlan;

  @Column(name = "code", unique = true, nullable = false, length = 50)
  String code;

  @Column(name = "name", nullable = false, length = 100)
  String name;

  @Column(name = "description", columnDefinition = "TEXT")
  String description;

  @Column(name = "price_annual", nullable = false, precision = 10, scale = 2)
  BigDecimal priceAnnual;

  @Column(name = "price_monthly", nullable = false, precision = 10, scale = 2)
  BigDecimal priceMonthly;

  @Column(name = "document_limit_yearly", nullable = false)
  Integer documentLimitYearly;

  @Column(name = "user_limit", nullable = false)
  Integer userLimit;

  @Column(name = "establishment_limit", nullable = false)
  Integer establishmentLimit;

  @Column(name = "issuance_point_limit", nullable = false)
  Integer issuancePointLimit;

  // Features
  @Column(name = "has_api_access", nullable = false)
  Boolean hasApiAccess;

  @Column(name = "has_multi_user", nullable = false)
  Boolean hasMultiUser;

  @Column(name = "has_advanced_reports", nullable = false)
  Boolean hasAdvancedReports;

  @Column(name = "has_electronic_invoice", nullable = false)
  Boolean hasElectronicInvoice;

  @Column(name = "has_credit_note", nullable = false)
  Boolean hasCreditNote;

  @Column(name = "has_debit_note", nullable = false)
  Boolean hasDebitNote;

  @Column(name = "has_retention", nullable = false)
  Boolean hasRetention;

  @Column(name = "has_guide", nullable = false)
  Boolean hasGuide;

  @Column(name = "has_priority_support", nullable = false)
  Boolean hasPrioritySupport;

  @Column(name = "has_custom_branding", nullable = false)
  Boolean hasCustomBranding;

  @Column(name = "has_automated_backups", nullable = false)
  Boolean hasAutomatedBackups;

  @Column(name = "is_active", nullable = false)
  Boolean isActive;

  @Column(name = "display_order", nullable = false)
  Integer displayOrder;
}
