package com.wallparisoft.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

	private Long idUser;
	private String userName;
	private String password;
	private String mail;
	private String name;
	private Boolean status;

	private List<RoleDto> roleDtos;
}
