package com.wallparisoft.ebill.product.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "product_tax_type")
@Data
public class ProductTaxType {

	@Id
	@SequenceGenerator(name = "product_tax_type_seq", sequenceName = "product_tax_type_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_tax_type_seq")
	@Column(name = "id_product_tax_type")
	private Long idProductTaxType;

	@ManyToOne
	@JoinColumn(name = "id_tax_type", referencedColumnName = "id_tax_type")
	private TaxType taxType;

	@ManyToOne
	@JoinColumn(name = "id_product", referencedColumnName = "id_product")
	private Product product;

	@Column(name = "status", nullable = false)
	private Boolean status;

	@Column(name = "creation_date", nullable = false)
	private LocalDateTime creationDate;

}
