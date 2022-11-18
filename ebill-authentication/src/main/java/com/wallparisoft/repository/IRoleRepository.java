package com.wallparisoft.repository;

import com.wallparisoft.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IRoleRepository extends JpaRepository<Role, Long> {

    @Query("FROM Role r where r.status = true order by r.name asc")
    List<Role> findRoleActive();

    @Query(value = "SELECT r FROM UserRole  ur inner join ur.role r inner join ur.user u where u.idUser = :idUser and ur.status=true and  u.status=true")
    List<Role> findRoleActiveByIdUser(@Param("idUser") Long idUser);

}
