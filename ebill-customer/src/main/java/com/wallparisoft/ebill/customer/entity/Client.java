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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "client")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class Client extends BaseEntity {

  @Id
  @SequenceGenerator(sequenceName = "client_seq", name = "client_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_seq")
  @Column(name = "id_client")
  private Long idClient;

  @Column(name = "id_type", nullable = false, length = 3)
  private Integer idType;

  @Column(name = "identification", nullable = false, length = 20, unique = true)
  private String identification;

  @Column(name = "name")
  private String name;

  @Column(name = "client_type", length = 3)
  private Integer clientType;
}
