package com.wallparisoft.ebill.customer.entity;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "company")
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

	@Column(name = "status", nullable = false)
	private boolean status;

	@Column(name = "creation_date")
	private LocalDateTime creationDate;

	@PrePersist
	public void preInsertar() {
		creationDate = LocalDateTime.now();
	}
}