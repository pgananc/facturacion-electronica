package com.wallparisoft.ebill.customer.controller;

import com.wallparisoft.ebill.customer.dto.ContactDto;
import com.wallparisoft.ebill.customer.entity.Company;
import com.wallparisoft.ebill.customer.entity.Contact;
import com.wallparisoft.ebill.customer.service.IContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/contact")
public class ContactController {

//	@Autowired
//	private IContactService contactService;
//
//	@PostMapping
//	public ResponseEntity<ContactDto> save(@Valid @RequestBody ContactDto contactDto) {
//		contactService.save(contactDto);
//		return new ResponseEntity<ContactDto>(contactDto, HttpStatus.CREATED);
//	}
//
//	@PutMapping("/{id}")
//	public ResponseEntity<ContactDto> update(@Valid @RequestBody ContactDto contactDto, @PathVariable Long id) {
//		contactService.update(contactDto, id);
//		return new ResponseEntity<ContactDto>(contactDto, HttpStatus.OK);
//	}
//
//	@GetMapping
//	public ResponseEntity<List<ContactDto>> findAll() {
//		List<ContactDto> contacts = deparmentService.findAll();
//		return new ResponseEntity<List<ContactDto>>(contacts, HttpStatus.OK);
//	}
//
//	@GetMapping("/{id}")
//	public ResponseEntity<ContactDto> findById(@PathVariable Long id) {
//		ContactDto contactDto = deparmentService.findById(id);
//		return new ResponseEntity<ContactDto>(contactDto, HttpStatus.OK);
//	}
}
