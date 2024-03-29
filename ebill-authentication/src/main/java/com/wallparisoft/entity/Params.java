package com.wallparisoft.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "params")
public class Params {


    @Id
    @SequenceGenerator(sequenceName = "params_seq", name = "params_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "params_seq")
    private Long idParam;

    @Column(name = "code_param", nullable = false, unique = true)
    private String codeParam;


    @Column(name = "value_param", nullable = false)
    private String valueParam;

    @Column(name = "description_param", nullable = false)
    private String descriptionParam;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @PrePersist
    public void preInsert() {
        creationDate = LocalDateTime.now();
    }

}
