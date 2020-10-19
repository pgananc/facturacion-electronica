package com.facturacion.electronica.modelo;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "forma_pago")
@Data
public class ProductoTipoImpuesto {

	@Id
	@SequenceGenerator(name = "forma_pago_seq", sequenceName = "forma_pago_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "forma_pago_seq")
	@Column(name = "id_forma_pago")
	private Long idFormaPago;

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

	@OneToMany(mappedBy = "formaPago")
	private List<ComprobanteVenta> comprobanteVenta;
}
