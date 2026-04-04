package com.wallparisoft.authentication.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {
  private String userName;
  private String password;
  private String mail;
  private String name;
  private Boolean status;
  private Long idCompany;
  private List<RoleDto> roleDtos;
}
