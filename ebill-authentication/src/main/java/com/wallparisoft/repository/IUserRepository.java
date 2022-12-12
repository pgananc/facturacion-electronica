package com.wallparisoft.repository;

import com.wallparisoft.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;


public interface IUserRepository extends JpaRepository<User, Long> {


  User findByUserName(String userName);

  @Query(value = "SELECT u FROM User u WHERE " +
          "UPPER(u.name) LIKE :name " +
          " AND UPPER(u.userName) like :userName  " +
          " AND u.status= :status " +
          "ORDER BY u.idUser ASC")
  Page<User> findUserByUserNameOrNameOrStatus(String name, String userName,
         Boolean status, Pageable pageable);

  @Query(value = "SELECT u FROM User  u WHERE u.status=true ORDER BY u.name")
  List<User> findUsersActive();

  boolean existsByUserName(String userName);

  User findByMailAndStatus(String mail, Boolean status);

  @Transactional
  @Modifying
  @Query("UPDATE User us SET us.password = :password WHERE us.idUser = :idUser")
  void changePassword(String password, Long idUser) throws Exception;

  @Query(value = "SELECT u FROM User u WHERE u.status=true AND u.idUser NOT IN (SELECT uc.user.idUser FROM UserCompany uc WHERE uc.company=:idCompany) ORDER BY u.name")
  Optional<List<User>> findActiveUsersAndNotInCompany(Long idCompany);
}
