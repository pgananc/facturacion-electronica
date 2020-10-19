package com.facturacion.electronica.modelo;

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
@Table(name = "cliente")
@Data
public class Cliente {
	@Id
	@SequenceGenerator(sequenceName = "cliente_seq", name = "cliente_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id_cliente")
	private Long idCliente;

	@Column(name = "tipo_identificacion", nullable = false, length = 3)
	private String tipoIdentificacion;

	@Column(name = "identificacion", nullable = false, length = 13)
	private String identificacion;

	@Column(name = "nombre")
	private String nombre;

	@Column(name = "tipo_cliente")
	private String tipoCliente;

	@Column(name = "estado", nullable = false)
	private String estado;

	@Column(name = "fecha_creacion")
	private LocalDateTime fechaCreacion;

	@OneToMany(mappedBy = "cliente")
	private List<ComprobanteVenta> comprobanteVenta;

	@PrePersist
	public void preInsertar() {
		fechaCreacion = LocalDateTime.now();
	}
}