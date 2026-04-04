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
@Table(name = "debit_note_payment")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DebitNotePayment extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "debit_note_payment_seq",
      sequenceName = "debit_note_payment_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "debit_note_payment_seq")
  @Column(name = "id_debit_note_payment")
  private Long idDebitNotePayment;

  @ManyToOne
  @JoinColumn(name = "id_debit_note", nullable = false)
  @JsonBackReference
  private DebitNote debitNote;

  @ManyToOne
  @JoinColumn(name = "id_payment_method", nullable = false)
  private PaymentMethod paymentMethod;

  @Column(name = "forma_pago", nullable = false, length = 2)
  private String formaPago;

  @Column(name = "total", nullable = false)
  private BigDecimal total;

  @Column(name = "term")
  private Integer term;

  @Column(name = "time_unit", length = 10)
  private String timeUnit;
}
