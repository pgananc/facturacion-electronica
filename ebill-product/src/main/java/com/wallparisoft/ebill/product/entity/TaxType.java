package com.wallparisoft.ebill.product.entity;

import java.math.BigDecimal;
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
@Table(name = "tax_type")
@Data
public class TaxType {

	@Id
	@SequenceGenerator(name = "tax_type_seq", sequenceName = "tax_type_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tax_type_seq")
	@Column(name = "id_tax_type")
	private Long idTaxType;

	@JoinColumn(name = "id_parent_tax_type", referencedColumnName = "id_tax_type")
	@ManyToOne
	private TaxType taxTypeParent;

	@Column(name = "name")
	private String name;

	@Column(name = "value")
	private BigDecimal value;

	@Column(name = "status", nullable = false)
	private Boolean status;

	@Column(name = "creation_date", nullable = false)
	private LocalDateTime creationDate;

}
