package com.facturacion.electronica.modelo;

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
@Table(name = "tipo_impuesto")
@Data
public class TipoImpuesto {

	@Id
	@SequenceGenerator(name = "tipo_impuesto_seq", sequenceName = "tipo_impuesto_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tipo_impuesto_seq")
	@Column(name = "id_tipo_impuesto")
	private Long idTipoImpuesto;

	@JoinColumn(name = "id_padre_tipo_impuesto", referencedColumnName = "id_tipo_impuesto")
	@ManyToOne
	private TipoImpuesto tipoImpuestoPadre;

	@Column(name = "nombre")
	private String nombre;

	@Column(name = "valor")
	private BigDecimal valor;

	@Column(name = "estado", nullable = false)
	private String estado;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

}
