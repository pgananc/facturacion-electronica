package com.wallparisoft.ebillbilling.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "purchase_proof")
@Data
public class PurchaseProof {

  @Id
  @SequenceGenerator(name = "purchase_proof_seq", sequenceName = "purchase_proof_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchase_proof_seq")
  @Column(name = "id_purchase_proof")
  private Long idPurchaseProof;

  @Column(name = "id_company")
  private Long idCompany;

  @ManyToOne
  @JoinColumn(name = "id_payment_method", referencedColumnName = "id_payment_method")
  private PaymentMethod paymentMethod;

  @Column(name = "id_client")
  private Long idClient;

  @Column(name = "creation_date", nullable = false)
  private LocalDateTime creationDate;

  @Column(name = "issue_date", nullable = false)
  private LocalDateTime issueDate;

  @Column(name = "access_key")
  private String accessKey;

  @Column(name = "purchase_proof_number")
  private String purchaseProofNumber;

  @Column(name = "environment")
  private String environment;

  @Column(name = "authorization_number")
  private String authorizationNumber;

  @Column(name = "additional_information")
  private String additionalInformation;

  @Column(name = "authorization_date")
  private LocalDateTime authorizationDate;

  @Column(name = "total_amount")
  private BigDecimal totalAmount;

  @Column(name = "tip")
  private BigDecimal tip;

  @Column(name = "subtotal_zero")
  private BigDecimal subtotalZero;

  @Column(name = "subtotal_iva")
  private BigDecimal subtotalIva;

  @Column(name = "subtotal_ice")
  private BigDecimal subtotalIce;

  @Column(name = "status")
  private String status;

  @PrePersist
  public void preInsert() {
    issueDate = LocalDateTime.now();
  }

}
