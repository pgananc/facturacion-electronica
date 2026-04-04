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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "subscription_usage_history")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
public class SubscriptionUsageHistory extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "subscription_usage_history_seq",
      sequenceName = "subscription_usage_history_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscription_usage_history_seq")
  @Column(name = "id_history")
  Long idHistory;

  @Column(name = "id_company", nullable = false)
  Long idCompany;

  @Column(name = "id_subscription", nullable = false)
  Long idSubscription;

  @ManyToOne
  @JoinColumn(
      name = "id_subscription",
      referencedColumnName = "id_subscription",
      insertable = false,
      updatable = false)
  CompanySubscription subscription;

  @Column(name = "month_year", nullable = false, length = 7)
  String monthYear; // Format: YYYY-MM

  @Column(name = "invoices_count", nullable = false)
  Integer invoicesCount;

  @Column(name = "credit_notes_count", nullable = false)
  Integer creditNotesCount;

  @Column(name = "debit_notes_count", nullable = false)
  Integer debitNotesCount;

  @Column(name = "retentions_count", nullable = false)
  Integer retentionsCount;

  @Column(name = "guides_count", nullable = false)
  Integer guidesCount;

  @Column(name = "total_documents", nullable = false)
  Integer totalDocuments;
}
