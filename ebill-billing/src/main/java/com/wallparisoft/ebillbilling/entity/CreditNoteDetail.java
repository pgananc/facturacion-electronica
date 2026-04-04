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
@Table(name = "credit_note_detail")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreditNoteDetail extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "credit_note_detail_seq",
      sequenceName = "credit_note_detail_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "credit_note_detail_seq")
  @Column(name = "id_credit_note_detail")
  private Long idCreditNoteDetail;

  @JoinColumn(name = "id_credit_note", referencedColumnName = "id_credit_note")
  @ManyToOne
  @JsonBackReference
  private CreditNote creditNote;

  @Column(name = "id_product")
  private Long idProduct;

  /** ID del detalle de factura original (para tracking de cantidades) */
  @Column(name = "id_invoice_detail")
  private Long idInvoiceDetail;

  @Column(name = "main_code", length = 25)
  private String mainCode;

  @Column(name = "aux_code", length = 25)
  private String auxCode;

  @Column(name = "description", length = 300)
  private String description;

  @Column(name = "unit_price")
  private BigDecimal unitPrice;

  @Column(name = "discount")
  private BigDecimal discount;

  @Column(name = "subtotal")
  private BigDecimal subtotal;

  @Column(name = "quantity")
  private Integer quantity;

  @Column(name = "iva_percentage")
  private BigDecimal ivaPercentage;

  @Column(name = "iva_value")
  private BigDecimal ivaValue;

  @Column(name = "ice_value")
  private BigDecimal iceValue;
}
