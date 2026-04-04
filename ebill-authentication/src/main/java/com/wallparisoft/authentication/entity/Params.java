package com.wallparisoft.authentication.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "params", schema = "public")
public class Params extends BaseEntity {

  @Id
  @SequenceGenerator(sequenceName = "params_seq", name = "params_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "params_seq")
  private Long idParam;

  @Column(name = "code_param", nullable = false, unique = true)
  private String codeParam;

  @Column(name = "value_param", nullable = false, columnDefinition = "text")
  private String valueParam;

  @Column(name = "description_param", nullable = false)
  private String descriptionParam;
}
