package com.facturacion.electronica.modelo;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "producto_tipo_impuesto")
@Data
public class FormaPago {

	@Id
	@SequenceGenerator(name = "producto_tipo_impuesto_seq", sequenceName = "producto_tipo_impuesto_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producto_tipo_impuesto_seq")
	@Column(name = "id_producto_tipo_impuesto")
	private Long idProductoTipoImpuesto;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

	@Column(name = "tipo_forma_pago")
	private String tipoFormaPago;

	@Column(name = "estado", nullable = false)
	private String estado;


}
