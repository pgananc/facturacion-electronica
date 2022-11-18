package com.wallparisoft.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wallparisoft.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IUserRepository extends JpaRepository<User, Long> {


  User findOneByUserName(String userName);

  @Query(value = "SELECT u FROM User  u where " +
          "UPPER(u.name) like :name " +
          " and UPPER(u.userName) like :userName  " +
          " and u.status= :status " +
          "order by u.idUser asc")
  Page<User> findUserByUserNameOrNameOrStatus(String name, String userName,
         Boolean status, Pageable pageable);

  @Query(value = "SELECT u FROM User  u where u.status=true order by u.name")
  List<User> findUsersActive();

  @Query(value = "SELECT u FROM User  u where u.idUser= :idUser order by u.name")
  List<User> findUserById(@Param(value = "idUser") Long idUser);

}
