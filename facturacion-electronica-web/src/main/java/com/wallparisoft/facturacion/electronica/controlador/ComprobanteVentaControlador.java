package com.wallparisoft.facturacion.electronica.controlador;

import java.io.Serializable;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.wallparisoft.facturacion.electronica.dto.ComprobanteVentaDto;

@ViewScoped
@Named
public class ComprobanteVentaControlador implements Serializable {

	private static final long serialVersionUID = -2765164024578345691L;
	private ComprobanteVentaDto comprobanteVentaDto;
	private List<ComprobanteVentaDto> listaComprobante;
	private String tipoComprobanteVenta;

	public void guardar() {
		comprobanteVentaDto = new ComprobanteVentaDto();
		comprobanteVentaDto.setTipoComprobante(tipoComprobanteVenta);
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<ComprobanteVentaDto> response = restTemplate.postForEntity(
				"http://localhost:8180/comprobante-venta", comprobanteVentaDto, ComprobanteVentaDto.class);
		System.out.println(response.getStatusCode());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void buscar() {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<List> response = restTemplate.getForEntity("http://localhost:8180/comprobante-venta",
				List.class);
		listaComprobante = response.getBody();
	}

	public ComprobanteVentaDto getComprobanteVentaDto() {
		return comprobanteVentaDto;
	}

	public void setComprobanteVentaDto(ComprobanteVentaDto comprobanteVentaDto) {
		this.comprobanteVentaDto = comprobanteVentaDto;
	}

	public void setTipoComprobanteVenta(String tipoComprobanteVenta) {
		this.tipoComprobanteVenta = tipoComprobanteVenta;
	}

	public String getTipoComprobanteVenta() {
		return tipoComprobanteVenta;
	}

	public List<ComprobanteVentaDto> getListaComprobante() {
		return listaComprobante;
	}

	public void setListaComprobante(List<ComprobanteVentaDto> listaComprobante) {
		this.listaComprobante = listaComprobante;
	}
}
