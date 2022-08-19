package com.wallparisoft.ebill.customer.controller;

import java.net.URI;
import java.util.List;

import com.wallparisoft.ebill.customer.entity.Company;
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

import com.wallparisoft.ebill.customer.service.ICompanyService;

@RestController
@RequestMapping("/company")
public class CompanyController {

	@Autowired
	private ICompanyService companyService;

	@GetMapping
	public ResponseEntity<List<Company>> findAll() {
		List<Company> lista = companyService.findAll();
		return new ResponseEntity<List<Company>>(lista, HttpStatus.OK);
	}

	@PostMapping
	public ResponseEntity<Object> registrar(@Validated @RequestBody Company company) {
		Company obj = companyService.save(company);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(obj.getIdCompany()).toUri();
		return ResponseEntity.created(location).build();
	}
}
