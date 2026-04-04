package com.wallparisoft.ebillbilling.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "company_subscription")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
public class CompanySubscription extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "company_subscription_seq",
      sequenceName = "company_subscription_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_subscription_seq")
  @Column(name = "id_subscription")
  Long idSubscription;

  @Column(name = "id_company", nullable = false)
  Long idCompany;

  @Column(name = "id_plan", nullable = false)
  Long idPlan;

  @ManyToOne
  @JoinColumn(
      name = "id_plan",
      referencedColumnName = "id_plan",
      insertable = false,
      updatable = false)
  SubscriptionPlan plan;

  @Column(name = "subscription_status", nullable = false, length = 20)
  String subscriptionStatus; // ACTIVE, EXPIRED, CANCELLED, SUSPENDED

  @Column(name = "start_date", nullable = false)
  LocalDateTime startDate;

  @Column(name = "expires_at")
  LocalDateTime expiresAt;

  @Column(name = "current_year_usage", nullable = false)
  Integer currentYearUsage;

  @Column(name = "last_reset_date", nullable = false)
  LocalDateTime lastResetDate;

  // Payment information
  @Column(name = "payment_method", length = 50)
  String paymentMethod;

  @Column(name = "last_payment_date")
  LocalDateTime lastPaymentDate;

  @Column(name = "last_payment_amount", precision = 10, scale = 2)
  BigDecimal lastPaymentAmount;

  @Column(name = "payment_reference")
  String paymentReference;

  @Column(name = "notes", columnDefinition = "TEXT")
  String notes;

  // Cancelación
  @Column(name = "cancelled_at")
  LocalDateTime cancelledAt;

  @Column(name = "cancel_reason", length = 50)
  String cancelReason; // PAYMENT_FAILURE, USER_REQUEST, VIOLATION, OTHER
}
