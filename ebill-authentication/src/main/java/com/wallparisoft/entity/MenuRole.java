package com.wallparisoft.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "menu_role")
public class MenuRole {

	@Id
	@SequenceGenerator(sequenceName = "user_role_seq", name = "user_role_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_role_seq")
	@Column(name = "id_menu_rol")
	private Long idMenuRole;

	@ManyToOne
	@JoinColumn(name = "id_menu")
	private Menu menu;

	@ManyToOne
	@JoinColumn(name = "id_role")
	private Role role;

	@Column(name = "status")
	private Boolean status;

}
