package com.wallparisoft.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wallparisoft.entity.User;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import javax.transaction.Transactional;
import java.util.List;


public interface IUserRepository extends JpaRepository<User, Long> {


  User findByUserName(String userName);

  @Query(value = "SELECT u FROM User  u where " +
          "UPPER(u.name) like :name " +
          " and UPPER(u.userName) like :userName  " +
          " and u.status= :status " +
          "order by u.idUser asc")
  Page<User> findUserByUserNameOrNameOrStatus(String name, String userName,
         Boolean status, Pageable pageable);

  @Query(value = "SELECT u FROM User  u where u.status=true order by u.name")
  List<User> findUsersActive();

  boolean existsByUserName(String userName);

  User findByMailAndStatus(String mail, Boolean status);

  @Transactional
  @Modifying
  @Query("UPDATE User us SET us.password = :password WHERE us.idUser = :idUser")
  void changePassword(String password, Long idUser) throws Exception;
}
