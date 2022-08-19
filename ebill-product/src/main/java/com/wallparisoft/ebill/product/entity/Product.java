package com.wallparisoft.ebill.product.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "product")
@Data
public class Product {
    @Id
    @SequenceGenerator(sequenceName = "product_seq", name = "product_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @Column(name = "id_product")
    private Long idProduct;

    @Column(name = "main_code")
    private String mainCode;

    @Column(name = "auxiliar_code")
    private String auxiliarCode;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "discount")
    private BigDecimal discount;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @PrePersist
    public void preInsertar() {
        creationDate = LocalDateTime.now();
    }
}