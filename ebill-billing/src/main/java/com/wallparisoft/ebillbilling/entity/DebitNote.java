package com.wallparisoft.ebillbilling.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "debit_note")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DebitNote extends BaseEntity {

  @Id
  @SequenceGenerator(name = "debit_note_seq", sequenceName = "debit_note_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "debit_note_seq")
  @Column(name = "id_debit_note")
  private Long idDebitNote;

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

  @Column(name = "debit_note_number")
  private String debitNoteNumber;

  @Column(name = "environment")
  private String environment;

  @Column(name = "additional_information", columnDefinition = "TEXT")
  private String additionalInformation;

  @Column(name = "total_amount")
  private BigDecimal totalAmount;

  @Column(name = "subtotal_zero")
  private BigDecimal subtotalZero;

  @Column(name = "subtotal_iva")
  private BigDecimal subtotalIva;

  @Column(name = "subtotal_ice")
  private BigDecimal subtotalIce;

  @Column(name = "discount")
  private BigDecimal discount;

  @Column(name = "total_iva")
  private BigDecimal totalIva;

  @Column(name = "total_ice")
  private BigDecimal totalIce;

  @Column(name = "valor_modificacion")
  private BigDecimal valorModificacion;

  @Column(name = "valor_total")
  private BigDecimal valorTotal;

  @Column(name = "observation", columnDefinition = "TEXT")
  private String observation;

  @Column(name = "email_sent")
  private Boolean emailSent;

  @Column(name = "email_sent_date")
  private LocalDateTime emailSentDate;

  // Campos específicos de Nota de Débito
  @Column(name = "original_invoice_id")
  private Long originalInvoiceId;

  @Column(name = "original_access_key", length = 49)
  private String originalAccessKey;

  @Column(name = "original_invoice_number", length = 17)
  private String originalInvoiceNumber;

  @Column(name = "original_issue_date")
  private LocalDateTime originalIssueDate;

  @Column(name = "document_type_modified", length = 2)
  private String documentTypeModified;

  @ManyToOne
  @JoinColumn(name = "id_authorization_sri", referencedColumnName = "id_authorization_sri")
  private AuthorizationSri authorizationSri;

  @ManyToOne
  @JoinColumn(
      name = "original_invoice_id",
      referencedColumnName = "id_invoice",
      insertable = false,
      updatable = false)
  @JsonIgnoreProperties({"invoiceDetails", "invoicePayments"})
  private Invoice originalInvoice;

  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "debitNote")
  private List<DebitNoteDetail> debitNoteDetails;

  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "debitNote")
  private List<DebitNotePayment> payments;
}
