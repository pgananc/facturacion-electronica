package com.wallparisoft.ebill.customer.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "company_contact")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyContact {
	@Id
	@SequenceGenerator(sequenceName = "company_contact_seq", name = "company_contact_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_contact_seq")
	@Column(name = "id_company_contact")
	private Long idCompanyContact;

	@ManyToOne
	@JoinColumn(name = "id_company", referencedColumnName = "id_company")
	private Company company;

	@ManyToOne
	@JoinColumn(name = "id_contact", referencedColumnName = "id_contact")
	private Contact contact;

	@Column(name = "status", nullable = false)
	private Boolean status;

	@Column(name = "creation_date")
	private LocalDateTime creationDate;

	@PrePersist
	public void preInsertar() {
		creationDate = LocalDateTime.now();
	}
}