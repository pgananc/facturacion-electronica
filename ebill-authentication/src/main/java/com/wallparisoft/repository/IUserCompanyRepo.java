package com.wallparisoft.repository;

import com.wallparisoft.entity.UserCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IUserCompanyRepo extends JpaRepository<UserCompany, Long> {
    Optional<UserCompany> findByIdCompanyAndUser_IdUser(@Param("idCompany") Long idCompany, @Param("idUser") Long idUser);

    Page<UserCompany> findByIdCompany(@Param("idCompany") Long idCompany, Pageable pageable);

    Optional<List<UserCompany>> findByIdCompany(@Param("idCompany") Long idCompany);

    Optional<List<UserCompany>> findByUser_IdUser(@Param("idUser") Long idUser);

    boolean existsByIdCompanyAndUser_UserName(@Param("idCompany") Long idCompany, @Param("userName") String userName);
}
