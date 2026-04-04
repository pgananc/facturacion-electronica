package com.wallparisoft.ebill.product.entity;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "product")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product extends BaseEntity {

  @Id
  @SequenceGenerator(sequenceName = "product_seq", name = "product_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
  @Column(name = "id_product")
  Long idProduct;

  @Column(name = "id_company", nullable = false)
  Long idCompany;

  @Column(name = "main_code")
  String mainCode;

  @Column(name = "name")
  String name;

  @Column(name = "product_type", length = 3)
  Integer productType;

  @Column(name = "unit_price")
  Double unitPrice;

  @Column(name = "description")
  String description;

  @Column(name = "has_ice")
  Boolean ice;

  @Column(name = "has_tourism_iva")
  Boolean tourismIva;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_iva_tax_type", nullable = false)
  TaxType ivaTaxType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_ice_tax_type")
  TaxType iceTaxType;
}
