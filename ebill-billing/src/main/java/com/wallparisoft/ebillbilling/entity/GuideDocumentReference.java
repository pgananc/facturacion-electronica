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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "guide_document_reference")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuideDocumentReference extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "guide_document_reference_seq",
      sequenceName = "guide_document_reference_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "guide_document_reference_seq")
  @Column(name = "id_guide_document_reference")
  private Long idGuideDocumentReference;

  @ManyToOne
  @JoinColumn(name = "id_guide", nullable = false)
  @JsonBackReference
  private Guide guide;

  @Column(name = "document_type", nullable = false, length = 2)
  private String documentType;

  @Column(name = "document_number", nullable = false, length = 17)
  private String documentNumber;

  @Column(name = "access_key", length = 49)
  private String accessKey;

  @Column(name = "issue_date")
  private LocalDateTime issueDate;
}
