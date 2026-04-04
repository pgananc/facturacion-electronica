package com.wallparisoft.authentication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "user_company_establishment")
@Data
@NoArgsConstructor
@SuperBuilder
public class UserCompanyEstablishment {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_company_establishment_seq")
  @SequenceGenerator(
      name = "user_company_establishment_seq",
      sequenceName = "user_company_establishment_seq",
      allocationSize = 1)
  private Long idUserCompanyEstablishment;

  @ManyToOne(optional = false)
  @JoinColumn(name = "id_user_company")
  private UserCompany userCompany;

  @Column(name = "id_establishment", nullable = false)
  private Long idEstablishment;
}
