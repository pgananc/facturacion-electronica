package com.wallparisoft.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallparisoft.model.Menu;
import com.wallparisoft.service.IMenuService;

@RestController
@RequestMapping("/menus")
public class MenuController {

	@Autowired
	private IMenuService service;

	@GetMapping(value = "/{userName}")
	public ResponseEntity<List<Menu>> listar(@PathVariable String userName) {
		List<Menu> menus = new ArrayList<>();
		menus = service.findMenuByUsername(userName);
		return new ResponseEntity<List<Menu>>(menus, HttpStatus.OK);
	}

}
