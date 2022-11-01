package com.wallparisoft.ebill.customer.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contact {

  @Id
  @SequenceGenerator(sequenceName = "id_contact_seq", name = "id_contact_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_contact_seq")
  @Column(name = "id_contact")
  private Long idContact;

  @Column(name = "contact_type", nullable = false, length = 3)
  private String contactType;

  @Column(name = "value")
  private String value;

  @Column(name = "status", nullable = false)
  private Boolean status;

  @Column(name = "creation_date")
  private LocalDateTime creationDate;

  @PrePersist
  public void preInsert() {
    creationDate = LocalDateTime.now();
  }
}