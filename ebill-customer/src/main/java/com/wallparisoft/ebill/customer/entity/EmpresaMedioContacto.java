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

import lombok.Data;

@Entity
@Table(name = "empresa_medio_contacto")
@Data
public class EmpresaMedioContacto {
	@Id
	@SequenceGenerator(sequenceName = "empresa_medio_contacto_seq", name = "empresa_medio_contacto_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "empresa_medio_contacto_seq")
	@Column(name = "id_empresa_medio_contacto")
	private Long idEmpresaMedioContacto;

	@ManyToOne
	@JoinColumn(name = "id_empresa", referencedColumnName = "id_empresa")
	private Empresa empresa;

	@ManyToOne
	@JoinColumn(name = "id_medio_contacto", referencedColumnName = "id_medio_contacto")
	private MedioContacto medioContacto;

	@Column(name = "estado", nullable = false)
	private String estado;

	@Column(name = "fecha_creacion")
	private LocalDateTime fechaCreacion;

	@PrePersist
	public void preInsertar() {
		fechaCreacion = LocalDateTime.now();
	}
}