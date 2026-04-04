package com.wallparisoft.ebillbilling.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "proforma")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Proforma {

  public enum Status {
    DRAFT,
    SENT,
    CONVERTED,
    EXPIRED
  }

  @Id
  @SequenceGenerator(name = "proforma_seq", sequenceName = "proforma_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "proforma_seq")
  @Column(name = "id_proforma")
  Long idProforma;

  @Column(name = "id_company", nullable = false)
  Long idCompany;

  @Column(name = "id_establishment", nullable = false)
  Long idEstablishment;

  @Column(name = "id_issuance_point", nullable = false)
  Long idIssuancePoint;

  @Column(name = "id_client")
  Long idClient;

  @Column(name = "proforma_number", nullable = false, length = 50)
  String proformaNumber;

  @Column(name = "issue_date", nullable = false)
  LocalDate issueDate;

  @Column(name = "valid_until")
  LocalDate validUntil;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  Status status;

  @Column(name = "id_invoice")
  Long idInvoice;

  @Column(name = "additional_information", length = 500)
  String additionalInformation;

  @Column(name = "created_at", nullable = false)
  LocalDateTime createdAt;

  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  @Column(name = "created_by", length = 100)
  String createdBy;

  @OneToMany(mappedBy = "proforma", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  List<ProformaDetail> details = new ArrayList<>();
}
