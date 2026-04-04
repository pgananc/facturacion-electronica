package com.wallparisoft.ebillbilling.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "proforma_detail")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProformaDetail {

  @Id
  @SequenceGenerator(
      name = "proforma_detail_seq",
      sequenceName = "proforma_detail_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "proforma_detail_seq")
  @Column(name = "id_proforma_detail")
  Long idProformaDetail;

  @ManyToOne
  @JoinColumn(name = "id_proforma", nullable = false)
  @JsonBackReference
  Proforma proforma;

  @Column(name = "id_product", nullable = false)
  Long idProduct;

  @Column(name = "product_code", length = 50)
  String productCode;

  @Column(name = "description", length = 500)
  String description;

  @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
  BigDecimal quantity;

  @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
  BigDecimal unitPrice;

  @Column(name = "discount", nullable = false, precision = 19, scale = 4)
  BigDecimal discount;

  @Column(name = "iva_percentage", nullable = false, precision = 5, scale = 2)
  BigDecimal ivaPercentage;

  @Column(name = "ice_percentage", nullable = false, precision = 5, scale = 2)
  BigDecimal icePercentage;

  @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
  BigDecimal subtotal;

  @Column(name = "line_order", nullable = false)
  Integer lineOrder;
}
