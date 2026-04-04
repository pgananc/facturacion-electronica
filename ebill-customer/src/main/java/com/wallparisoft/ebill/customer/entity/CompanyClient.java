package com.wallparisoft.ebill.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "company_client")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyClient {
  @Id
  @SequenceGenerator(
      sequenceName = "company_client_seq",
      name = "company_client_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_client_seq")
  @Column(name = "id_company_client")
  private Long idCompanyClient;

  @Column(name = "company_identification")
  private String companyIdentification;

  @ManyToOne
  @JoinColumn(name = "id_client", referencedColumnName = "id_client")
  private Client client;

  @Column(name = "creation_date")
  private LocalDateTime creationDate;

  @PrePersist
  private void preInsert() {
    creationDate = LocalDateTime.now();
  }
}
