package com.wallparisoft.repository;

import com.wallparisoft.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;


public interface IUserRepository extends JpaRepository<User, Long> {


  User findByUserName(String userName);

  @Query(value = "SELECT u FROM UserCompany uc inner join uc.user u WHERE uc.idCompany =:idCompany and " +
          "UPPER(u.name) LIKE :name " +
          " AND UPPER(u.userName) like :userName  " +
          " AND u.status= :status " +
          "ORDER BY u.idUser ASC")
  Page<User> findUserByIdCompanyUserNameOrNameOrStatus(Long idCompany, String name, String userName,
         Boolean status, Pageable pageable);

  @Query(value = "SELECT u FROM UserCompany uc inner join uc.user u WHERE uc.idCompany= :idCompany and  u.status=true ORDER BY u.name")
  List<User> findUsersByCompanyAndStatusActive(@Param("idCompany") Long idCompany);

  User findByMailAndStatus(String mail, Boolean status);

  @Transactional
  @Modifying
  @Query("UPDATE User us SET us.password = :password WHERE us.idUser = :idUser")
  void changePassword(String password, Long idUser) throws Exception;

  @Query(value = "SELECT u FROM User u WHERE u.status=true AND u.idUser NOT IN (SELECT uc.user.idUser FROM UserCompany uc WHERE uc.idCompany=:idCompany) ORDER BY u.name")
  Optional<List<User>> findActiveUsersAndNotInCompany(Long idCompany);
}
