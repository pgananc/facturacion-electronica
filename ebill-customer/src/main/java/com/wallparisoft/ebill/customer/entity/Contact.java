package com.wallparisoft.ebill.customer.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Contact extends BaseEntity {

  @Id
  @SequenceGenerator(sequenceName = "contact_seq", name = "contact_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contact_seq")
  @Column(name = "id_contact")
  private Long idContact;

  @Column(name = "contact_type", nullable = false, length = 3)
  private String contactType;

  @Column(name = "value")
  private String value;
}
