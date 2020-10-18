package com.wallparisoft.facturacion.electronica.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ComprobanteVentaDto {
	private Integer idComprobanteVenta;
	private String tipoComprobante;
	private LocalDateTime fechaGeneracion;

}
