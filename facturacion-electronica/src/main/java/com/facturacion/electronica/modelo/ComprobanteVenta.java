package com.facturacion.electronica.modelo;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "comprobante_venta")
@Data
public class ComprobanteVenta {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_comprobante_venta")
	private Integer idComprobanteVenta;

	@Column(name = "tipo_comprobante", nullable = false, length = 50)
	private String tipoComprobante;

	@Column(name = "fecha_generacion")
	private LocalDateTime fechaGeneracion;

	@PrePersist
	public void preInsertar() {
		fechaGeneracion = LocalDateTime.now();
	}

}
