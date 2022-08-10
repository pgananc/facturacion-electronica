package com.wallparisoft.facturacion.electronica.controlador;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.wallparisoft.facturacion.electronica.modelo.ComprobanteVenta;
import com.wallparisoft.facturacion.electronica.servicio.IComprobanteVentaServicio;

@RestController
@RequestMapping("/comprobante-venta")
public class ComprobanteVentaController {

	@Autowired
	private IComprobanteVentaServicio comprobanteVentaServicio;

	@GetMapping
	public ResponseEntity<List<ComprobanteVenta>> listar() {
		List<ComprobanteVenta> lista = comprobanteVentaServicio.listar();
		return new ResponseEntity<List<ComprobanteVenta>>(lista, HttpStatus.OK);
	}

	@PostMapping
	public ResponseEntity<Object> registrar(@Validated @RequestBody ComprobanteVenta comprobanteVenta) {
		ComprobanteVenta obj = comprobanteVentaServicio.registrar(comprobanteVenta);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(obj.getIdComprobanteVenta()).toUri();
		return ResponseEntity.created(location).build();
	}
}
