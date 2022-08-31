package com.wallparisoft.ebill.customer.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "client_contact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientContact {

  @Id
  @SequenceGenerator(sequenceName = "client_contact_seq", name = "client_contact_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_contact_seq")
  @Column(name = "id_client_contact")
  private Long idClientContact;

  @ManyToOne
  @JoinColumn(name = "id_client", referencedColumnName = "id_client")
  private Client client;

  @ManyToOne
  @JoinColumn(name = "id_contact", referencedColumnName = "id_contact")
  private Contact contact;

  @Column(name = "status", nullable = false)
  private boolean status;

  @Column(name = "creation_date")
  private LocalDateTime creationDate;

  @PrePersist
  public void preInsert() {
    creationDate = LocalDateTime.now();
  }
}