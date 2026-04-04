package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.User;
import com.wallparisoft.authentication.entity.UserCompany;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IUserCompanyRepo extends JpaRepository<UserCompany, Long> {
  Optional<UserCompany> findByIdCompanyAndUser_IdUser(
      @Param("idCompany") Long idCompany, @Param("idUser") Long idUser);

  Optional<UserCompany> findByUser_IdUserAndIdCompany(Long idUser, Long idCompany);

  Page<UserCompany> findByIdCompany(@Param("idCompany") Long idCompany, Pageable pageable);

  Optional<List<UserCompany>> findByIdCompany(@Param("idCompany") Long idCompany);

  List<UserCompany> findByUser(User user);

  Optional<List<UserCompany>> findByUser_IdUser(@Param("idUser") Long idUser);

  boolean existsByUser_UserName(@Param("userName") String userName);

  boolean existsByUser_Mail(@Param("mail") String mail);

  @Query(
          value =
                  "SELECT uc FROM UserCompany uc inner join uc.user u WHERE uc.idCompany =:idCompany and "
                          + "UPPER(u.name) LIKE :name "
                          + " AND UPPER(u.userName) like :userName  "
                          + " AND u.status= :status "
                          + "ORDER BY u.idUser ASC")
  Page<UserCompany> findUserByIdCompanyUserNameOrNameOrStatus(
          Long idCompany, String name, String userName, Boolean status, Pageable pageable);

  @Query(
          value =
                  "SELECT uc FROM UserCompany uc inner join uc.user u WHERE uc.idCompany= :idCompany and  u.status=true ORDER BY u.name")
  List<UserCompany> findUsersByCompanyAndStatusActive(@Param("idCompany") Long idCompany);
}
