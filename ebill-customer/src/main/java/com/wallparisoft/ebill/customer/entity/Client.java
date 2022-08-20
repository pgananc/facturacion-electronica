package com.wallparisoft.ebill.customer.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "client")
@Data
public class Client {

    @Id
    @SequenceGenerator(sequenceName = "client_seq", name = "client_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_seq")
    @Column(name = "id_client")
    private Long idClient;

    @Column(name = "id_type", nullable = false, length = 3)
    private String id_type;

    @Column(name = "identification", nullable = false, length = 13)
    private String identification;

    @Column(name = "name")
    private String name;

    @Column(name = "client_type")
    private String clientType;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "id_company")
    private Long idCompany;

    @PrePersist
    public void preInsert() {
        creationDate = LocalDateTime.now();
    }
}