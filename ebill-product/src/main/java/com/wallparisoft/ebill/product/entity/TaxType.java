package com.wallparisoft.ebill.product.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "tax_type")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaxType extends BaseEntity {

  @Id
  @SequenceGenerator(name = "tax_type_seq", sequenceName = "tax_type_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tax_type_seq")
  @Column(name = "id_tax_type")
  Long idTaxType;

  @Column(name = "type")
  String type;

  @Column(name = "name")
  String name;

  @Column(name = "value")
  BigDecimal value;
}
