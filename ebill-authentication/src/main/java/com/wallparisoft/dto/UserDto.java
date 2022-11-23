package com.wallparisoft.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDto {

	private Long idUser;
	private String userName;
	private String password;
	private String mail;
	private String name;
	private Boolean status;

	private List<RoleDto> roleDtos;
}
