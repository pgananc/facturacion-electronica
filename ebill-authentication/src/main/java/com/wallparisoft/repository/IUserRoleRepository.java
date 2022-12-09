package com.wallparisoft.repository;

import com.wallparisoft.entity.User;
import com.wallparisoft.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IUserRoleRepository extends JpaRepository<UserRole, Long> {

    UserRole findByUser_IdUserAndAndRole_IdRole(@Param("idUser") Long idUser,@Param("idRole") Long idRole);

    void deleteUserRoleByUser(User user);

    List<UserRole> findByUser_UserNameAndStatus(@Param("userName") String userName, @Param("status") Boolean status);
}
