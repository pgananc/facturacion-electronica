package com.wallparisoft.ebill.customer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "company", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"identification", "branch_office_code"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {
    @Id
    @SequenceGenerator(sequenceName = "company_seq", name = "company_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_seq")
    @Column(name = "id_company")
    private Long idCompany;

    @Column(name = "id_type", nullable = false, length = 3)
    private Integer idType;

    @Column(name = "identification", nullable = false, length = 13)
    private String identification;

    @Column(name = "name")
    private String name;

    @Column(name = "branch_office_code")
    private String branchOfficeCode;

    @Column(name = "forced_to_accounting")
    private String forcedToAccounting;

    @Column(name = "special_taxpayer")
    private Long specialTaxpayer;

    @Column(name = "principal")
    private String principal;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @PrePersist
    public void preInsertar() {
        creationDate = LocalDateTime.now();
    }
}