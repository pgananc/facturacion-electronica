package com.wallparisoft.repository;

import com.wallparisoft.entity.UserCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IUserCompanyRepo extends JpaRepository<UserCompany, Long> {
    Optional<UserCompany> findByCompanyAndUser_IdUser(@Param("idCompany") Long idCompany, @Param("idUser") Long idUser);

    Page<UserCompany> findByCompany(@Param("idCompany") Long idCompany, Pageable pageable);

    Optional<List<UserCompany>> findByCompany(@Param("idCompany") Long idCompany);
}
