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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "guide_additional_information")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuideAdditionalInformation extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "guide_additional_information_seq",
      sequenceName = "guide_additional_information_seq",
      allocationSize = 1)
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "guide_additional_information_seq")
  @Column(name = "id_guide_additional_information")
  private Long idGuideAdditionalInformation;

  @ManyToOne
  @JoinColumn(name = "id_guide", nullable = false)
  @JsonBackReference
  private Guide guide;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "description", nullable = false, length = 500)
  private String description;
}
