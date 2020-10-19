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
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "comprobante_venta")
@Data
public class ComprobanteVenta {
	@Id
	@SequenceGenerator(name = "comprobante_venta_seq", sequenceName = "comprobante_venta_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comprobante_venta_seq")
	@Column(name = "id_comprobante_venta")
	private Long idComprobanteVenta;

	@ManyToOne
	@JoinColumn(name = "id_empresa", referencedColumnName = "id_empresa")
	private Empresa empresa;

	@ManyToOne
	@JoinColumn(name = "id_forma_pago", referencedColumnName = "id_forma_pago")
	private FormaPago formaPago;

	@ManyToOne
	@JoinColumn(name = "id_cliente", referencedColumnName = "id_cliente")
	private Cliente cliente;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

	@Column(name = "fecha_emision", nullable = false)
	private LocalDateTime fechaEmision;

	@Column(name = "clave_acceso")
	private String claveAcceso;

	@Column(name = "numero_comprobante_venta")
	private String numeroComprobanteVenta;

	@Column(name = "ambiente")
	private String ambiente;

	@Column(name = "numero_autorizacion")
	private String numeroAutorizacion;

	@Column(name = "informacion_adicional")
	private String informacionAdicional;

	@Column(name = "fecha_autorizacion")
	private LocalDateTime fechaAutorizacion;

	@Column(name = "valor_total")
	private BigDecimal valorTotal;

	@Column(name = "propina")
	private BigDecimal propina;

	@Column(name = "subtotal_cero")
	private BigDecimal subtotalCero;

	@Column(name = "subtotal_iva")
	private BigDecimal subtotalIva;

	@Column(name = "subtotal_ice")
	private BigDecimal subtotalIce;

	@Column(name = "estado")
	private String estado;

	@PrePersist
	public void preInsertar() {
		fechaEmision = LocalDateTime.now();
	}

}
