package com.wallparisoft.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wallparisoft.dto.UserDto;
import com.wallparisoft.model.User;
import com.wallparisoft.service.ILoginService;

@RestController
@RequestMapping("/login")
public class LoginController {

	@Autowired
	private ILoginService service;

	@PostMapping
	public ResponseEntity<Integer> login(@RequestBody UserDto userDto) {
		int rpta = 0;
		try {
			User us = service.validateUserName(userDto.getUserName(), userDto.getPassword());
			if (us != null) {
				rpta = 1;
			}
		} catch (Exception e) {
			return new ResponseEntity<Integer>(rpta, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Integer>(rpta, HttpStatus.OK);
	}

}
