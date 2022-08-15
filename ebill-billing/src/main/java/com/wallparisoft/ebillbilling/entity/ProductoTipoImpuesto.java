package com.wallparisoft.ebillbilling.entity;

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
@Table(name = "producto_tipo_impuesto")
@Data
public class ProductoTipoImpuesto {

	@Id
	@SequenceGenerator(name = "producto_tipo_impuesto_seq", sequenceName = "producto_tipo_impuesto_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producto_tipo_impuesto_seq")
	@Column(name = "id_producto_tipo_impuesto")
	private Long idProductoTipoImpuesto;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

	@ManyToOne
	@JoinColumn(name = "id_tipo_impuesto", referencedColumnName = "id_tipo_impuesto")
	private TipoImpuesto tipoImpuesto;

	@ManyToOne
	@JoinColumn(name = "id_producto", referencedColumnName = "id_producto")
	private Producto producto;

	@Column(name = "estado", nullable = false)
	private String estado;

}
