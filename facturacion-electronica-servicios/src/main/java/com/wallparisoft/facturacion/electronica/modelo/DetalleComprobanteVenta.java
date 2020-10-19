package com.wallparisoft.facturacion.electronica.modelo;

import java.math.BigDecimal;
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
@Table(name = "detalle_comprobante_venta")
@Data
public class DetalleComprobanteVenta {

	@Id
	@SequenceGenerator(name = "detalle_comprobante_venta_seq", sequenceName = "detalle_comprobante_venta_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "detalle_comprobante_venta_seq")
	@Column(name = "id_detalle_comprobante_venta")
	private Long idDetalleComprobanteVenta;

	@JoinColumn(name = "id_comprobante_venta", referencedColumnName = "id_comprobante_venta")
	@ManyToOne
	private ComprobanteVenta comprobanteVenta;

	@ManyToOne
	@JoinColumn(name = "id_producto", referencedColumnName = "id_producto")
	private Producto producto;

	@Column(name = "precio_unitario")
	private BigDecimal precioUnitario;

	@Column(name = "descuento")
	private BigDecimal descuento;

	@Column(name = "subtotal")
	private BigDecimal subtotal;

	@Column(name = "cantidad")
	private BigDecimal cantidad;

	@Column(name = "estado", nullable = false)
	private String estado;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

}
