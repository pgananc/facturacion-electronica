package com.wallparisoft.dto;

import lombok.Data;

@Data
public class RoleDto{
    private Long idRole;
    private String name;
    private String description;
    private Boolean status;
}
