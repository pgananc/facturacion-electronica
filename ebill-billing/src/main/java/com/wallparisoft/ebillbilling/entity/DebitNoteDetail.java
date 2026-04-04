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
@Table(name = "debit_note_detail")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DebitNoteDetail extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "debit_note_detail_seq",
      sequenceName = "debit_note_detail_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "debit_note_detail_seq")
  @Column(name = "id_debit_note_detail")
  private Long idDebitNoteDetail;

  @ManyToOne
  @JoinColumn(name = "id_debit_note", nullable = false)
  @JsonBackReference
  private DebitNote debitNote;

  @Column(name = "id_product")
  private Long idProduct;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Column(name = "unit_price", nullable = false)
  private BigDecimal unitPrice;

  @Column(name = "discount")
  private BigDecimal discount;

  @Column(name = "subtotal", nullable = false)
  private BigDecimal subtotal;
}
