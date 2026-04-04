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
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "guide")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Guide extends BaseEntity {

  @Id
  @SequenceGenerator(name = "guide_seq", sequenceName = "guide_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "guide_seq")
  @Column(name = "id_guide")
  private Long idGuide;

  @Column(name = "id_company", nullable = false)
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

  @Column(name = "issue_date", nullable = false)
  private LocalDateTime issueDate;

  @Column(name = "guide_number", length = 17)
  private String guideNumber;

  @Column(name = "environment", length = 10)
  private String environment;

  @Column(name = "additional_information", columnDefinition = "TEXT")
  private String additionalInformation;

  @Column(name = "email_sent")
  private Boolean emailSent;

  @Column(name = "email_sent_date")
  private LocalDateTime emailSentDate;

  // Campos específicos de guía de remisión
  @Column(name = "start_date", nullable = false)
  private LocalDateTime startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDateTime endDate;

  @Column(name = "transport_type", length = 50)
  private String transportType;

  @Column(name = "transport_plate", length = 20)
  private String transportPlate;

  @Column(name = "id_carrier", nullable = false, insertable = true, updatable = true)
  private Long idCarrier;

  @Column(name = "origin_address", nullable = false, length = 300)
  private String originAddress;

  @Column(name = "destination_address", nullable = false, length = 300)
  private String destinationAddress;

  @Column(name = "reason_for_transfer", length = 300)
  private String reasonForTransfer;

  @Column(name = "route", length = 300)
  private String route;

  // Relaciones
  @ManyToOne
  @JoinColumn(name = "id_authorization_sri", referencedColumnName = "id_authorization_sri")
  private AuthorizationSri authorizationSri;

  @ManyToOne
  @JoinColumn(
      name = "id_carrier",
      referencedColumnName = "id_carrier",
      insertable = false,
      updatable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private Carrier carrier;

  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "guide")
  @JsonIgnoreProperties({"guide"})
  private List<GuideDetail> guideDetails;

  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "guide")
  @JsonIgnoreProperties({"guide"})
  private List<GuideDocumentReference> documentReferences;

  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "guide")
  @JsonIgnoreProperties({"guide"})
  private List<GuideAdditionalInformation> additionalInformationList;
}
