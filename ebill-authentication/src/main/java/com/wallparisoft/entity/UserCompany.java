package com.wallparisoft.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "user_company")
public class UserCompany {

  @Id
  @Column(name = "id_user_company")
  @SequenceGenerator(sequenceName = "user_company_seq", name = "user_company_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_company_seq")
  private Long idUserCompany;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(referencedColumnName = "id_user", name = "id_user")
  private User user;

  @Column(name = "id_company", nullable = false)
  private Long company;

}
