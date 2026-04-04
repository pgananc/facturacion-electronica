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
@Table(name = "guide_detail")
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuideDetail extends BaseEntity {

  @Id
  @SequenceGenerator(
      name = "guide_detail_seq",
      sequenceName = "guide_detail_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "guide_detail_seq")
  @Column(name = "id_guide_detail")
  private Long idGuideDetail;

  @ManyToOne
  @JoinColumn(name = "id_guide", nullable = false)
  @JsonBackReference
  private Guide guide;

  @Column(name = "id_product")
  private Long idProduct;

  @Column(name = "description", nullable = false, length = 300)
  private String description;

  @Column(name = "quantity", nullable = false)
  private BigDecimal quantity;

  @Column(name = "unit", length = 10)
  private String unit;

  @Column(name = "weight")
  private BigDecimal weight;

  @Column(name = "additional_info", columnDefinition = "TEXT")
  private String additionalInfo;
}
