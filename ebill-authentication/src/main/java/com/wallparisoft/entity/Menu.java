package com.wallparisoft.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "menu")
public class Menu {

	@Id
	@SequenceGenerator(sequenceName = "menu_seq", name = "menu_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "menu_seq")
	private Integer idMenu;

	@Column(name = "icon", length = 20)
	private String icon;

	@Column(name = "name", length = 20)
	private String name;

	@Column(name = "url", length = 50)
	private String url;

	@Column(name = "status", nullable = false)
	private Boolean status;

}
