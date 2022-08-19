package com.wallparisoft.ebill.customer.entity;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "client")
@Data
public class Client {

  @Id
  @SequenceGenerator(sequenceName = "client_seq", name = "client_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_seq")
  @Column(name = "id_client")
  private Long idClient;

  @Column(name = "id_type", nullable = false, length = 3)
  private String id_type;

  @Column(name = "identification", nullable = false, length = 13)
  private String identification;

  @Column(name = "name")
  private String name;

  @Column(name = "client_type")
  private String clientType;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "creation_date")
  private LocalDateTime creationDate;

  @PrePersist
  public void preInsert() {
    creationDate = LocalDateTime.now();
  }
}