package com.wallparisoft.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_company")
public class UserCompany {

    @Id
    @Column(name = "id_user_company")
    @SequenceGenerator(sequenceName = "user_company_seq", name = "user_company_seq", allocationSize = 1)
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