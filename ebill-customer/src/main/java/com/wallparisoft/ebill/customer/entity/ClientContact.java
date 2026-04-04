package com.wallparisoft.ebill.customer.entity;

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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "client_contact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class ClientContact extends BaseEntity {

  @Id
  @SequenceGenerator(
      sequenceName = "client_contact_seq",
      name = "client_contact_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_contact_seq")
  @Column(name = "id_client_contact")
  private Long idClientContact;

  @ManyToOne
  @JoinColumn(name = "id_client", referencedColumnName = "id_client")
  private Client client;

  @ManyToOne
  @JoinColumn(name = "id_contact", referencedColumnName = "id_contact")
  private Contact contact;
}
