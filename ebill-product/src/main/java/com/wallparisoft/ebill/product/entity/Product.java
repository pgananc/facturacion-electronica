package com.wallparisoft.ebill.product.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {

  @Id
  @SequenceGenerator(sequenceName = "product_seq", name = "product_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
  @Column(name = "id_product")
  private Long idProduct;

  @Column(name = "main_code")
  private String mainCode;

  @Column(name = "auxiliar_code")
  private String auxiliarCode;

  @Column(name = "product_type")
  private String productType;

  @Column(name = "name")
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "unit_price")
  private Double unitPrice;

  @Column(name = "discount")
  private Double discount;

  @Column(name = "status", nullable = false)
  private boolean status;

  @Column(name = "creation_date")
  private LocalDateTime creationDate;

  @PrePersist
  public void preInsert() {
    creationDate = LocalDateTime.now();
  }
}