package com.wallparisoft.ebillbilling.entity;

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

import lombok.Data;

@Entity
@Table(name = "medio_contacto")
@Data
public class MedioContacto {
	@Id
	@SequenceGenerator(sequenceName = "medio_contacto_seq", name = "medio_contacto_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "medio_contacto_seq")
	@Column(name = "id_medio_contacto")
	private Long idMedioContacto;

	@Column(name = "tipo_medio_contacto", nullable = false, length = 3)
	private String tipoMedioContacto;

	@Column(name = "valor")
	private String valor;

	@Column(name = "estado", nullable = false)
	private String estado;

	@Column(name = "fecha_creacion")
	private LocalDateTime fechaCreacion;

	@OneToMany(mappedBy = "medioContacto")
	private List<EmpresaMedioContacto> empresaMedioContactos;

	@OneToMany(mappedBy = "medioContacto")
	private List<ClienteMedioContacto> clienteMedioContactos;

	@PrePersist
	public void preInsertar() {
		fechaCreacion = LocalDateTime.now();
	}
}