package com.wallparisoft.ebill.product.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {

    @Id
    @SequenceGenerator(sequenceName = "product_seq", name = "product_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @Column(name = "id_product")
    Long idProduct;

    @Column(name = "main_code")
    String mainCode;

    @Column(name = "auxiliar_code")
    String auxiliarCode;

    @Column(name = "product_type", length = 3)
    Integer productType;

    @Column(name = "name")
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "unit_price")
    Double unitPrice;

    @Column(name = "discount")
    Double discount;

    @Column(name = "status", nullable = false)
    boolean status;

    @Column(name = "id_company", nullable = false)
    Long idCompany;

    @Column(name = "creation_date")
    LocalDateTime creationDate;

    @PrePersist
    void preInsert() {
        creationDate = LocalDateTime.now();
    }
}