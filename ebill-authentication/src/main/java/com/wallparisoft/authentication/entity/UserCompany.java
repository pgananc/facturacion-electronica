package com.wallparisoft.authentication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_company", schema = "public")
public class UserCompany {

  @Id
  @Column(name = "id_user_company")
  @SequenceGenerator(
      sequenceName = "user_company_seq",
      name = "user_company_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_company_seq")
  private Long idUserCompany;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_user", referencedColumnName = "idUser")
  private User user;

  @Column(name = "id_company", nullable = false)
  private Long idCompany;

  @Column(name = "creation_date")
  private LocalDateTime creationDate;

  @PrePersist
  private void preInsert() {
    creationDate = LocalDateTime.now();
  }
}
