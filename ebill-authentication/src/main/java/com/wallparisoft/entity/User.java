package com.wallparisoft.entity;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.*;

import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

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

  @Column(name = "mail", nullable = false)
  private String mail;

  @Column(name = "status", nullable = false)
  private Boolean status;

  @Column(name = "creation_date")
  private LocalDateTime creationDate;

  @Column(name = "update_date")
  private LocalDateTime updateDate;
  @PrePersist
  public void preInsert() {
    creationDate = LocalDateTime.now();
  }


}
