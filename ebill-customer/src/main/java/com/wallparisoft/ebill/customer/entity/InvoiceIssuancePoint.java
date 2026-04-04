package com.wallparisoft.ebill.customer.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
@Entity
@EqualsAndHashCode(callSuper = false)
@Table(name = "invoice_issuance_point")
public class InvoiceIssuancePoint extends BaseEntity {
  @Id
  @SequenceGenerator(
      sequenceName = "invoice_issuance_point_seq",
      name = "invoice_issuance_point_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_issuance_point_seq")
  @Column(name = "id_invoice_issuance_point", nullable = false)
  private Long idInvoiceIssuancePoint;

  @Size(max = 100)
  @Column(name = "code", length = 100)
  private String code;

  @Column(name = "sequential_invoice")
  private Long sequentialInvoice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_establishment")
  private Establishment establishment;
}
