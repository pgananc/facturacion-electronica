package com.wallparisoft.authentication.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "user_company_role",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_user_company_role",
          columnNames = {"userCompany", "role"})
    })
@Data
@NoArgsConstructor
@SuperBuilder
public class UserCompanyRole {

  @Id
  @SequenceGenerator(
      sequenceName = "user_company_role_seq",
      name = "user_company_role_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_company_role_seq")
  private Long idUserCompanyRole;

  @ManyToOne(optional = false)
  @JoinColumn(name = "id_user_company", nullable = false)
  private UserCompany userCompany;

  @ManyToOne(optional = false)
  @JoinColumn(name = "id_role", nullable = false)
  private Role role;

  private boolean status;
}
