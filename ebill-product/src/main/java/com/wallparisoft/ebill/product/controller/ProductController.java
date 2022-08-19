package com.wallparisoft.ebill.product.controller;

import java.net.URI;
import java.util.List;

import com.wallparisoft.ebill.product.entity.Product;
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

import com.wallparisoft.ebill.product.service.IProductService;

@RestController
@RequestMapping("/product")
public class ProductController {

	@Autowired
	private IProductService productService;

	@GetMapping
	public ResponseEntity<List<Product>> listar() {
		List<Product> lista = productService.findAll();
		return new ResponseEntity<List<Product>>(lista, HttpStatus.OK);
	}

	@PostMapping
	public ResponseEntity<Object> registrar(@Validated @RequestBody Product product) {
		Product obj = productService.save(product);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(obj.getIdProduct()).toUri();
		return ResponseEntity.created(location).build();
	}
}
