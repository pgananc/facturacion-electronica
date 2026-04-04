package com.wallparisoft.authentication.entity;

import com.wallparisoft.ebill.commons.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "users", schema = "public")
public class User extends BaseEntity {

  @Id
  @SequenceGenerator(sequenceName = "user_seq", name = "user_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
  private Long idUser;

  @Column(name = "name")
  private String name;

  @Column(name = "username", nullable = false, unique = true)
  private String userName;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "password_encrypted", length = 500)
  private String passwordEncrypted;

  @Column(name = "mail", nullable = false)
  private String mail;
}
