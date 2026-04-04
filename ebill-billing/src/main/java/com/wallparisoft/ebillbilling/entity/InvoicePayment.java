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
@Table(name = "invoice_payment")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoicePayment extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "invoice_payment_seq",
      sequenceName = "invoice_payment_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_payment_seq")
  @Column(name = "id_invoice_payment")
  private Long idInvoicePayment;

  @ManyToOne
  @JoinColumn(name = "id_invoice", referencedColumnName = "id_invoice", nullable = false)
  @JsonBackReference
  private Invoice invoice;

  @ManyToOne
  @JoinColumn(name = "id_payment_method", referencedColumnName = "id_payment_method")
  private PaymentMethod paymentMethod; // Desde aquí obtenemos el paymentMethodCode

  @Column(name = "total", precision = 38, scale = 2)
  private BigDecimal total; // Monto de esta forma de pago

  @Column(name = "term")
  private Integer term; // Plazo

  @Column(name = "time_unit", length = 50)
  private String timeUnit; // Unidad de tiempo (ej: "dias", "DÍAS")
}
