package com.wallparisoft.authentication.repository;

import com.wallparisoft.authentication.entity.UserCompany;
import com.wallparisoft.authentication.entity.UserCompanyRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IUserCompanyRoleRepository extends JpaRepository<UserCompanyRole, Long> {

  UserCompanyRole findUserCompanyRoleByUserCompany_IdUserCompanyAndRole_IdRole(
      Long idUserCompany, Long idRole);

  void deleteByUserCompany(UserCompany userCompany);

  List<UserCompanyRole> findByUserCompany_User_UserNameAndStatus(String username, Boolean status);

  List<UserCompanyRole> findByUserCompany(UserCompany userCompany);

  List<UserCompanyRole> findByUserCompanyAndStatus(UserCompany userCompany, Boolean status);
}
