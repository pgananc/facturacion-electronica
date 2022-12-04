package com.wallparisoft.ebill.customer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_client")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyClient {
    @Id
    @SequenceGenerator(sequenceName = "company_client_seq", name = "company_client_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_client_seq")
    @Column(name = "id_company_client")
    private Long idCompanyClient;

    @ManyToOne
    @JoinColumn(name = "id_company", referencedColumnName = "id_company")
    private Company company;

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