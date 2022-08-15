package com.wallparisoft.ebill.product.entity;

import java.math.BigDecimal;
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
@Table(name = "producto")
@Data
public class Producto {
	@Id
	@SequenceGenerator(sequenceName = "producto_seq", name = "producto_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producto_seq")
	@Column(name = "id_producto")
	private Long idProducto;

	@Column(name = "codigo_principal")
	private String codigoPrincipal;

	@Column(name = "codigo_auxiliar")
	private String codigoAuxiliar;

	@Column(name = "tipo_producto")
	private String tipoProducto;

	@Column(name = "nombre")
	private String nombre;

	@Column(name = "descripcion")
	private String descripcion;

	@Column(name = "valor_unitario")
	private BigDecimal valorUnitario;

	@Column(name = "descuento")
	private BigDecimal descuento;

	@Column(name = "estado", nullable = false)
	private String estado;

	@Column(name = "fecha_creacion")
	private LocalDateTime fechaCreacion;

	@PrePersist
	public void preInsertar() {
		fechaCreacion = LocalDateTime.now();
	}
}