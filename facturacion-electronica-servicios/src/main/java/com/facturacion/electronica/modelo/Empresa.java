package com.facturacion.electronica.modelo;

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
@Table(name = "empresa")
@Data
public class Empresa {
	@Id
	@SequenceGenerator(sequenceName = "empresa_seq", name = "empresa_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id_empresa")
	private Long idEmpresa;

	@Column(name = "tipo_identificacion", nullable = false, length = 3)
	private String tipoIdentificacion;

	@Column(name = "identificacion", nullable = false, length = 13)
	private String identificacion;

	@Column(name = "nombre")
	private String nombre;

	@Column(name = "codigo_sucursal")
	private String codigoSucursal;

	@Column(name = "obligado_llevar_contabilidad")
	private String obligadoLlevarContabilidad;

	@Column(name = "contribuyente_especial")
	private Long contribuyenteEspecial;

	@Column(name = "principal")
	private String principal;

	@Column(name = "estado", nullable = false)
	private String estado;

	@Column(name = "fecha_creacion")
	private LocalDateTime fechaCreacion;

	@PrePersist
	public void preInsertar() {
		fechaCreacion = LocalDateTime.now();
	}
}