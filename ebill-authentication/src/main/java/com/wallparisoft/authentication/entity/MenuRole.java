package com.wallparisoft.authentication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "menu_role", schema = "public")
public class MenuRole {

  @Id
  @SequenceGenerator(sequenceName = "menu_role_seq", name = "menu_role_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "menu_role_seq")
  @Column(name = "id_menu_role")
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
