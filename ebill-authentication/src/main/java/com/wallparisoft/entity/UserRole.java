package com.wallparisoft.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "user_role")
public class UserRole {

	@Id
	@SequenceGenerator(sequenceName = "user_role_seq", name = "user_role_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_role_seq")
	@Column(name = "id_user_rol")
	private Long idUserRole;

	@ManyToOne
	@JoinColumn(name = "id_user")
	private User user;

	@ManyToOne
	@JoinColumn(name = "id_role")
	private Role role;

	@Column(name = "status")
	private Boolean status;

}
