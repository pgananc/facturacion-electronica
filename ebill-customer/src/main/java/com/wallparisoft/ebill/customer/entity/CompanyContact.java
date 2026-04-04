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
@Table(name = "company_contact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class CompanyContact extends BaseEntity {
  @Id
  @SequenceGenerator(
      sequenceName = "company_contact_seq",
      name = "company_contact_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_contact_seq")
  @Column(name = "id_company_contact")
  private Long idCompanyContact;

  @ManyToOne
  @JoinColumn(name = "id_company", referencedColumnName = "id_company")
  private Company company;

  @ManyToOne
  @JoinColumn(name = "id_contact", referencedColumnName = "id_contact")
  private Contact contact;
}
