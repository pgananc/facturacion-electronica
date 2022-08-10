package com.wallparisoft.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "rol")
public class Rol {

	@Id
	@SequenceGenerator(sequenceName = "rol_seq", name = "rol_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rol_seq")
	private Long idRol;

	@Column(name = "name")
	private String name;

	@Column(name = "description")
	private String description;

}
