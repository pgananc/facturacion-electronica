package com.wallparisoft.ebillbilling.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "invoice_detail")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDetail extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "invoice_detail_seq",
      sequenceName = "invoice_detail_detail_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_detail_seq")
  @Column(name = "id_invoice_detail")
  private Long idInvoiceDetail;

  @JoinColumn(name = "id_invoice", referencedColumnName = "id_invoice")
  @ManyToOne
  @JsonBackReference
  private Invoice invoice;

  @Column(name = "id_product")
  private Long idProduct;

  @Column(name = "unit_price")
  private BigDecimal unitPrice;

  @Column(name = "discount")
  private BigDecimal discount;

  @Column(name = "subtotal")
  private BigDecimal subtotal;

  @Column(name = "quantity")
  private Integer quantity;
}
