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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "purchase_proof_detail")
@Data
public class PurchaseProofDetail {

  @Id
  @SequenceGenerator(name = "purchase_proof_detail_seq", sequenceName = "purchase_proof_detail_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchase_proof_detail_seq")
  @Column(name = "id_purchase_proof_detail")
  private Long idPurchaseProofDetail;

  @JoinColumn(name = "id_purchase_proof", referencedColumnName = "id_purchase_proof")
  @ManyToOne
  private PurchaseProof purchaseProof;

  @Column(name = "id_product")
  private Long idProduct;

  @Column(name = "unit_price")
  private BigDecimal unitPrice;

  @Column(name = "discount")
  private BigDecimal discount;

  @Column(name = "subtotal")
  private BigDecimal subtotal;

  @Column(name = "quantity")
  private BigDecimal quantity;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "creation_date", nullable = false)
  private LocalDateTime creationDate;

}