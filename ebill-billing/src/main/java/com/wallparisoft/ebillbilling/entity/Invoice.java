package com.wallparisoft.ebillbilling.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "invoice")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Invoice extends BaseEntity {

  @Id
  @SequenceGenerator(name = "invoice_seq", sequenceName = "invoice_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_seq")
  @Column(name = "id_invoice")
  private Long idInvoice;

  @Column(name = "id_company")
  private Long idCompany;

  @Column(name = "id_client")
  private Long idClient;

  @Column(name = "id_establishment")
  private Long idEstablishment;

  @Column(name = "id_issuance_point")
  private Long idIssuancePoint;

  @Column(name = "sequential")
  private Long sequential;

  @Column(name = "retry_count")
  private Integer retryCount;

  @Column(name = "issue_date")
  private LocalDateTime issueDate;

  @Column(name = "invoice_number")
  private String invoiceNumber;

  @Column(name = "environment")
  private String environment;

  @Column(name = "additional_information")
  private String additionalInformation;

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

  @Column(name = "discount")
  private BigDecimal discount;

  @Column(name = "email_sent")
  private Boolean emailSent;

  @Column(name = "email_sent_date")
  private LocalDateTime emailSentDate;

  @ManyToOne
  @JoinColumn(name = "id_authorization_sri", referencedColumnName = "id_authorization_sri")
  private AuthorizationSri authorizationSri;

  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "invoice")
  private List<InvoiceDetail> invoiceDetails;

  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "invoice")
  private List<InvoicePayment> invoicePayments;
}
