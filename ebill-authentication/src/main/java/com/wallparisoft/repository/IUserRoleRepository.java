package com.wallparisoft.repository;

import com.wallparisoft.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IUserRoleRepository extends JpaRepository<UserRole, Long> {

    @Query(value = "SELECT ur FROM UserRole  ur where  ur.user.idUser =:idUser and ur.role.idRole = :idRole")
    Optional<UserRole> findByIdUserAndIdRole(@Param("idUser") Long idUser,@Param("idRole") Long idRole);


}
