package com.wallparisoft.ebill.customer.controller;

import java.net.URI;
import java.util.List;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.entity.Company;
import com.wallparisoft.ebill.customer.response.BasicResponse;
import com.wallparisoft.ebill.customer.response.ClientDtoResponse;
import com.wallparisoft.ebill.customer.response.CompanyDtoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.wallparisoft.ebill.customer.service.ICompanyService;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

	@Autowired
	private ICompanyService companyService;

	@PostMapping
	public ResponseEntity<BasicResponse> save(@Valid @RequestBody CompanyDto companyDto) {
		companyService.saveCompanyAndContact(companyDto);
		BasicResponse response = BasicResponse.builder().status("OK").build();
		return new ResponseEntity<>(response, HttpStatus.CREATED);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<BasicResponse> delete(@PathVariable("id") Long id) {
		companyService.delete(id);
		BasicResponse response = BasicResponse.builder().status("OK").build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping
	public ResponseEntity<CompanyDtoResponse> findCompaniesActiveAndContactActive() {
		List<CompanyDto> companies = companyService.findCompaniesActiveAndContactActive();
		CompanyDtoResponse response = CompanyDtoResponse.builder()
				.status("OK")
				.companyDtos(companies)
				.build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PutMapping
	public ResponseEntity<BasicResponse> update(@Valid @RequestBody CompanyDto companyDto) {
		companyService.updateCompanyAndContact(companyDto);
		BasicResponse response = BasicResponse.builder().status("OK").build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
